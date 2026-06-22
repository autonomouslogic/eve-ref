package com.autonomouslogic.everef.esi;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Response;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Singleton
@Log4j2
public class EsiHelper {
	private static final String PAGES_HEADER = "X-Pages";
	private static final String COMPATIBILITY_DATE_HEADER = "X-Compatibility-Date";

	@Inject
	@Named("esi")
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected EsiHelper() {}

	/**
	 * Fetches the requested URL.
	 * This call does NOT include standard error handling.
	 * @param url
	 * @return
	 */
	public Response fetch(EsiUrl url) {
		return fetch(url, Optional.empty());
	}

	/**
	 * Fetches the requested URL.
	 * This call does NOT include standard error handling.
	 * @param url
	 * @return
	 */
	public Response fetch(EsiUrl url, Optional<String> accessToken) {
		return okHttpWrapper.get(url.toString(), r -> {
			accessToken.ifPresent(token -> r.addHeader("Authorization", "Bearer " + token));
			r.addHeader(
					COMPATIBILITY_DATE_HEADER,
					LocalDate.now(ZoneOffset.ofHours(-11)).toString());
		});
	}

	/**
	 * Fetches the requested URL and automatically fetches all subsequent pages indicated by the header.
	 * This call does NOT include standard error handling.
	 * @param url
	 * @return
	 */
	@SneakyThrows
	protected List<Response> fetchPages(EsiUrl url, Optional<String> accessToken) {
		var first = fetch(url.toBuilder().page(1).build(), accessToken);
		var pages = first.header(PAGES_HEADER);
		if (pages == null || pages.isEmpty()) {
			return List.of(first);
		}
		var pagesInt = Integer.parseInt(pages);
		if (pagesInt == 1) {
			return List.of(first);
		}
		var allResponses = new ArrayList<Response>();
		allResponses.add(first);

		// Fetch remaining pages in parallel using virtual threads
		var pageNumbers = new ArrayList<Integer>();
		for (int i = 2; i <= pagesInt; i++) {
			pageNumbers.add(i);
		}

		var tasks = pageNumbers.stream()
				.map(page -> (Callable<Response>) () -> {
					var esiUrl = url.toBuilder().page(page).build();
					return fetchWithRetry(esiUrl, accessToken);
				})
				.toList();

		VirtualThreads.checkIsVirtual();
		var remainingPages = VirtualThreads.callAll(tasks.iterator(), tasks.size());

		allResponses.addAll(remainingPages);

		return allResponses;
	}

	/**
	 * Fetches a URL with automatic retry on failure.
	 */
	private Response fetchWithRetry(EsiUrl url, Optional<String> accessToken) {
		var attempt = 0;
		while (attempt < 3) {
			try {
				return fetch(url, accessToken);
			} catch (Exception e) {
				if (e instanceof EsiException) {
					throw e;
				}
				attempt++;
				if (attempt >= 3) {
					log.warn("Failed to fetch {} after retries: {}", url, ExceptionUtils.getMessage(e));
					throw new RuntimeException(String.format("Failed to fetch %s", url), e);
				}
				try {
					Thread.sleep(Duration.ofSeconds(2).toMillis());
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Interrupted while retrying fetch", ie);
				}
			}
		}
		throw new RuntimeException("Unexpected state in fetchWithRetry");
	}

	protected List<Response> fetchPages(EsiUrl url) {
		return fetchPages(url, Optional.empty());
	}

	/**
	 * Like {@link #fetchPages(EsiUrl)}, but works on the OpenAPI generated ESI API classes.
	 * @param fetcher
	 * @return
	 * @param <T>
	 */
	@SneakyThrows
	public <T> List<T> fetchPages(Function<Integer, ApiResponse<List<T>>> fetcher) {
		var first = fetchWithRetry(fetcher, 1);
		var pages = Optional.ofNullable(first.getHeaders().get("X-Pages")).stream()
				.flatMap(List::stream)
				.mapToInt(Integer::valueOf)
				.findFirst()
				.orElse(1);
		var result = new ArrayList<>(decodeResponse(first));

		if (pages > 1) {
			var pageNumbers = new ArrayList<Integer>();
			for (int i = 2; i <= pages; i++) {
				pageNumbers.add(i);
			}

			var tasks = pageNumbers.stream()
					.map(page -> (Callable<List<T>>) () -> decodeResponse(fetchWithRetry(fetcher, page)))
					.toList();

			VirtualThreads.checkIsVirtual();
			var remainingPages = VirtualThreads.callAll(tasks.iterator(), tasks.size()).stream()
					.flatMap(List::stream)
					.toList();

			result.addAll(remainingPages);
		}

		return result;
	}

	/**
	 * Fetches API response with automatic retry.
	 */
	@SneakyThrows
	private <T> ApiResponse<List<T>> fetchWithRetry(Function<Integer, ApiResponse<List<T>>> fetcher, int page) {
		var attempt = 0;
		while (attempt < 3) {
			try {
				return fetcher.apply(page);
			} catch (Exception e) {
				attempt++;
				if (attempt >= 3) {
					throw new RuntimeException(String.format("Failed to fetch page %d after retries", page), e);
				}
				try {
					Thread.sleep(Duration.ofSeconds(1).toMillis());
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Interrupted while retrying fetch", ie);
				}
			}
		}
		throw new RuntimeException("Unexpected state in fetchWithRetry");
	}

	/**
	 * Fetches multiple pages of a URL, decoding each page as a JSON array, and returning the objects in a stream.
	 * @param url
	 * @return
	 */
	public Flowable<JsonNode> fetchPagesOfJsonArrays(EsiUrl url) {
		return fetchPagesOfJsonArrays(url, (a, b) -> a);
	}

	/**
	 * Fetches multiple pages of a URL, decoding each page as a JSON array, and returning the objects in a stream.
	 * This call includes standard error handling.
	 * @param url
	 * @return
	 */
	@SneakyThrows
	public Flowable<JsonNode> fetchPagesOfJsonArrays(
			EsiUrl url, BiFunction<JsonNode, Response, JsonNode> augmenter, Optional<String> accessToken) {
		var responses = fetchPages(url, accessToken);
		// Apply error handling and decode all responses immediately to avoid holding sockets open during async
		// processing
		var filteredResponses = filterResponsesByErrorStatus(responses, url);
		var allEntries = new ArrayList<JsonNode>();

		for (var response : filteredResponses) {
			var node = decodeResponse(response);
			var arrayEntries = decodeArrayNode(url, node);
			for (var arrayEntry : arrayEntries) {
				allEntries.add(augmenter.apply(arrayEntry, response));
			}
		}

		return Flowable.fromIterable(allEntries);
	}

	public Flowable<JsonNode> fetchPagesOfJsonArrays(EsiUrl url, BiFunction<JsonNode, Response, JsonNode> augmenter) {
		return fetchPagesOfJsonArrays(url, augmenter, Optional.empty());
	}

	/**
	 * Decodes a JsonNode from a generic response body.
	 * @param response
	 * @return
	 */
	@SneakyThrows
	public JsonNode decodeResponse(Response response) {
		try (response) {
			if (response.code() == 204) {
				return NullNode.getInstance();
			}
			if (response.code() == 404) {
				log.warn("404 response from {}", response.request().url());
				return NullNode.getInstance();
			}
			if (response.code() != 200) {
				throw new RuntimeException(String.format("Cannot decode non-200 response: %s", response.code()));
			}
			return objectMapper.readTree(response.peekBody(Long.MAX_VALUE).byteStream());
		}
	}

	/**
	 * Dismantles the elements from a JSON array.
	 * @param url
	 * @param node
	 * @return
	 */
	public List<JsonNode> decodeArrayNode(EsiUrl url, JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			log.warn("Empty response from {}", url);
			return List.of();
		}
		if (!node.isArray()) {
			throw new RuntimeException(String.format("Expected array, got %s - %s", node.getNodeType(), url));
		}
		var result = new ArrayList<JsonNode>();
		node.forEach(result::add);
		return result;
	}

	/**
	 * Filters responses by error status, applying standard error handling.
	 * Throws EsiException for 401/403 responses.
	 * Skips 204, 404, 5xx (except 520), and other 4xx responses.
	 * @param responses
	 * @param url
	 * @return
	 */
	private List<Response> filterResponsesByErrorStatus(List<Response> responses, EsiUrl url) {
		var filtered = new ArrayList<Response>();
		for (var response : responses) {
			var status = response.code();
			if (status == 401 || status == 403) {
				throw new EsiException(status, String.format("Received %s for %s", status, url));
			}
			if (status == 204 || status == 404 || (status / 100 == 5 && status != 520)) {
				log.debug("Ignoring {} response received for {}", status, url);
				continue;
			} else if (status / 100 == 4) {
				log.warn("Ignoring {} response received for {}", status, url);
				continue;
			}
			filtered.add(response);
		}
		return filtered;
	}

	/**
	 * Extracts a result from an OpenAPI generated ESI API response.
	 * @param response
	 * @return
	 * @param <T>
	 */
	private <T> T decodeResponse(ApiResponse<T> response) {
		int status = response.getStatusCode();
		if (status / 100 != 2) {
			throw new RuntimeException(String.format("Unexpected response: %s", status, response));
		}
		return response.getData();
	}

	/**
	 * Populates the JSON entry with the Last-Modified header from the response.
	 * The timestamp stored is ISO-8601.
	 * @param entry
	 * @param response
	 * @return
	 */
	public JsonNode populateLastModified(JsonNode entry, Response response) {
		var lastModified = okHttpWrapper.getLastModified(response);
		var obj = (ObjectNode) entry;
		lastModified.ifPresent(
				date -> obj.put("http_last_modified", date.toInstant().toString()));
		return obj;
	}

	/**
	 * @deprecated Use {@link EsiHelper#filterResponsesByErrorStatus(List, EsiUrl)} instead for blocking error handling.
	 * @param url
	 * @return
	 */
	@Deprecated(forRemoval = true)
	public StandardErrorTransformer standardErrorHandling(EsiUrl url) {
		return new StandardErrorTransformer(url);
	}
}
