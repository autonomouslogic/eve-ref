package com.autonomouslogic.everef.scrape;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.inject.Inject;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Fetches items from the ESI.
 */
@Log4j2
public class ScrapeFetcher {
	public static final String ESI_420_TEXT = "This software has exceeded the error limit for ESI.";

	@Inject
	protected OkHttpClient httpClient;
	@Inject
	protected ObjectMapper jsonObjectMapper;

	private EsiScraper esiScraper;
	private AtomicInteger totalItems = new AtomicInteger();
	private Meter requestMeter = new Meter();
	private Meter cacheHitMeter = new Meter();
	private Meter notModifiedMeter = new Meter();
	private Meter cacheMissMeter = new Meter();
	private Meter notFoundMeter = new Meter();
	private Meter completedItems = new Meter();
	private Meter skippedItems = new Meter();
	private final ExecutorService httpPool;

	@Inject
	public ScrapeFetcher(EsiConfig esiConfig) {
		httpPool = Executors.newFixedThreadPool(esiConfig.getMaxConnections(), new ThreadFactoryBuilder()
			.setNameFormat(getClass().getSimpleName() + "-%d")
			.build()
		);
	}

	/**
	 * Fetches the requested URL and returns the response as a {@link JsonNode}.
	 * @param url
	 * @return
	 */
	public CompletableFuture<Response> fetch(EsiUrl url, String etag) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				boolean fetched = false;
				Response response = null;
				int tries = 0;
				List<String> errors = new ArrayList<>();
				while (!fetched) {
					if (tries == 10) {
						long unacceptables = errors.stream()
							.filter(e -> !e.equals("status:420"))
							.filter(e -> !e.equals("error:timeout"))
							.count();
						if (unacceptables == 0) {
							log.warn(String.format("Skipping URL due to 420 responses: %s", url.toString()));
							skippedItems.mark();
							return null;
						}
						else {
							throw new RuntimeException(String.format("Retries exceeded for URL %s: %s", url.toString(), errors.toString()));
						}
					}
//					while (requestMeter.getOneMinuteRate() > maxRequestRate) {
//						Thread.sleep(1000);
//					}
					tries++;
//					if (etag != null) { // @todo validate this is properly handled by OkHttp on ESI scrape. Also, remove this from the ESI scrape.
//						requestBuilder.addHeader("If-None-Match", etag);
//					}
					String body = null;
					try {
						requestMeter.mark();
						Request.Builder requestBuilder = new Request.Builder()
							.get()
							.url(url.toString());
						response = httpClient.newCall(requestBuilder.build()).execute();
						body = response.peekBody(Long.MAX_VALUE).string();
					}
					catch (Exception e) {
						Throwable root = ExceptionUtils.getRootCause(e);
						String rootMessage = ExceptionUtils.getRootCauseMessage(e);
						if (root instanceof TimeoutException || root instanceof SocketTimeoutException) {
							log.info(String.format("%s: %s - %s", root.getClass().getSimpleName(), rootMessage, url));
							errors.add("error:timeout");
							Thread.sleep(1000);
							continue;
						}
						if (root instanceof SocketException) {
							log.info(String.format("%s: %s - %s", root.getClass().getSimpleName(), rootMessage, url));
							errors.add("error:socket");
							Thread.sleep(1000);
							continue;
						}
						if (root instanceof SSLException) {
							log.info(String.format("%s: %s - %s", root.getClass().getSimpleName(), rootMessage, url));
							errors.add("error:ssl");
							Thread.sleep(1000);
							continue;
						}
						if (root instanceof StreamResetException) {
							log.info(String.format("%s: %s - %s", root.getClass().getSimpleName(), rootMessage, url));
							errors.add("error:streamreset");
							Thread.sleep(1000);
							continue;
						}
						if (rootMessage.toLowerCase().contains("canceled")) {
							log.info(String.format("%s: %s - %s", root.getClass().getSimpleName(), rootMessage, url));
							errors.add("error:canceled");
							Thread.sleep(1000);
							continue;
						}
						errors.add("error:" + root.getClass().getSimpleName());
						log.warn("Error on scrape", e);
						Thread.sleep(1000);
						continue;
					}
					if (response == null) {
						errors.add("response:null");
						Thread.sleep(1000);
						continue;
					}
					if (response.code() == 420 || (body != null && body.contains(ESI_420_TEXT))) {
						errors.add("status:420");
						String resetTimeString = response.header("X-Esi-Error-Limit-Reset");
						int resetTime = 10;
						if (resetTimeString != null) {
							resetTime += Integer.parseInt(resetTimeString);
						}
						log.info(String.format("420 on scrape, waiting %s: %s", resetTime, url));
						Thread.sleep(resetTime * 1000);
						continue;
					}
					if (response.code() == 504) {
						errors.add("status:504");
						log.info(String.format("504 on scrape: %s", url));
						tries--;
						Thread.sleep(1000);
						continue;
					}
					if (response.code() == 520) {
						int resetTime = 60;
						log.info(String.format("520 on scrape, waiting %s: %s - %s",
							resetTime,
							url,
							Optional.ofNullable(body).orElse("")
						));
						Thread.sleep(resetTime * 1000);
						return response;
					}
					if (response.code() >= 500 && response.code() < 600) {
						log.info(String.format("%s (500-599) on scrape: %s", response.code(), url));
						errors.add("status:" + response.code());
						Thread.sleep(1000);
						continue;
					}
					if (response.code() == 403) {
						log.info(String.format("403 on scrape: %s", url));
						return response;
					}
					if (response.code() == 404) {
						log.info(String.format("404 on scrape: %s", url));
						return response;
					}
					if (response.code() == 304) {
						return response;
					}
					if (response.code() != 200 && response.code() != 204) {
						throw new RuntimeException(String.format("Invalid status: %s from %s", response.code(), url));
					}
					fetched = true;
				}
				return response;
			}
			catch (Exception e) {
				throw new RuntimeException(String.format("Failed fetching URL %s", url), e);
			}
		}, httpPool).thenApplyAsync(v -> v);
	}


	public CompletableFuture<ArrayNode> fetchAllPagesAndDecode(EsiUrl url) {
		return fetchAllPages(url)
			.thenApply(responses -> mergeArrays(responses.stream()
				.map(this::decode)
				.collect(Collectors.toList())
			));
	}

	/**
	 * Fetches all results for a multi-page endpoint.
	 * @param url
	 * @return
	 */
	public CompletableFuture<List<Response>> fetchAllPages(EsiUrl url) {
		// First page.
		return fetch(url, null).thenCompose(first -> {
			String pages = first.header("X-Pages");
			if (pages == null || pages.isEmpty()) {
				return completedFuture(Collections.singletonList(first));
			}
			int pagesInt = Integer.parseInt(pages);
			if (pagesInt == 1) {
				return completedFuture(Collections.singletonList(first));
			}
			Response[] responses = new Response[pagesInt];
			responses[0] = first;
			CompletableFuture[] futures = new CompletableFuture[pagesInt - 1];
			for (int p=2 ; p<=pagesInt ; p++) {
				final int page = p;
				futures[p-2] = fetch(new EsiUrl(url).setPage(p), null).thenAccept(response -> {
					responses[page - 1] = response;
				});
			}
			return allOf(futures).thenApply(ignore -> {
				return Arrays.asList(responses);
			});
		});
	}

	public CompletableFuture<JsonNode> fetchDecodeAndWrite(EsiUrl url) {
//		// Wait for queue size.
//		while (totalItems.get() - completedItems.getCount() > maxQueueSize) {
//			try {
//				Thread.sleep(25); // @todo blocking the common pool :(
//			}
//			catch (InterruptedException e) {
//				throw new RuntimeException(e);
//			}
//		}
		totalItems.incrementAndGet();
		// Load cached data.
		UrlData urlData = esiScraper.loadUrlData(url);
		boolean hasData = urlData != null && urlData.getData() != null && urlData.getData().length > 0;
		String etag = urlData == null || !hasData ? null : urlData.getEtag();
		boolean cacheExpired = urlData == null || urlData.getExpire() == null || Instant.now().isAfter(urlData.getExpire());
		// If we have a file already and the meta isn't expired, simply return the content.
		if (hasData && !cacheExpired) {
			try {
				cacheHitMeter.mark();
				completedItems.mark();
				return completedFuture(jsonObjectMapper.readTree(urlData.getData()));
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		// Cache couldn't be used, fetch the URL.
		return fetch(url, etag)
			.thenApply(response -> {
				// Check not found.
				if (response == null || response.code() == 404) {
					notFoundMeter.mark();
					return null;
				}
				// Check not modified.
				if (etag != null && response.code() == 304) {
					notModifiedMeter.mark();
					try {
						byte[] data = esiScraper.loadUrlData(url).getData();
						JsonNode json = jsonObjectMapper.readTree(data);
						UrlData cachedUrlData = UrlData.fromResponse(url, response)
							.setEtag(etag)
							.setData(data);
						esiScraper.saveUrlData(cachedUrlData);
						return json;
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				cacheMissMeter.mark();
				// Save meta.
				esiScraper.saveUrlData(UrlData.fromResponse(url, response));
				// Decode response normally.
				return decode(response);
			})
			.thenApply(json -> {
				completedItems.mark();
				return json;
			});
	}

	public JsonNode decode(Response response) {
		try {
			return jsonObjectMapper.readTree(response.peekBody(Long.MAX_VALUE).byteStream());
		}
		catch (Exception e) {
			throw new RuntimeException("Decoding failed", e);
		}
	}

	public ArrayNode mergeArrays(Collection<JsonNode> arrays) {
		ArrayNode array = jsonObjectMapper.createArrayNode();
		for (JsonNode node : arrays) {
			array.addAll((ArrayNode) node);
		}
		return array;
	}

	public AtomicInteger getTotalItems() {
		return totalItems;
	}

	public Meter getCacheHitMeter() {
		return cacheHitMeter;
	}

	public Meter getNotModifiedMeter() {
		return notModifiedMeter;
	}

	public Meter getCacheMissMeter() {
		return cacheMissMeter;
	}

	public Meter getNotFoundMeter() {
		return notFoundMeter;
	}

	public Meter getCompletedItems() {
		return completedItems;
	}

	public Meter getRequestMeter() {
		return requestMeter;
	}

	public Meter getSkippedItems() {
		return skippedItems;
	}

	public EsiScraper getEsiScraper() {
		return esiScraper;
	}

	public ScrapeFetcher setEsiScraper(EsiScraper esiScraper) {
		this.esiScraper = esiScraper;
		return this;
	}
}
