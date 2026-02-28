package com.autonomouslogic.everef.esi;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiResponse;
import com.autonomouslogic.everef.util.VirtualThreads;
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
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Response;

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
		return Flowable.concatArray(
						Flowable.just(first),
						Flowable.range(2, pagesInt - 1)
								.parallel(4)
								.runOn(VirtualThreads.SCHEDULER)
								.flatMap(page -> Flowable.just(
										fetch(url.toBuilder().page(page).build(), accessToken)))
								.sequential())
				.toList()
				.blockingGet();
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
	public <T> Flowable<T> fetchPages(Function<Integer, ApiResponse<List<T>>> fetcher) {
		return Flowable.defer(() -> Flowable.just(fetcher.apply(1)))
				.flatMap(first -> {
					var pages = Optional.ofNullable(first.getHeaders().get("X-Pages")).stream()
							.flatMap(List::stream)
							.mapToInt(Integer::valueOf)
							.findFirst()
							.orElse(1);
					var firstResult = decodeResponse(first);
					if (pages == 1) {
						return Flowable.fromIterable(firstResult);
					}
					return Flowable.concatArray(
							Flowable.fromIterable(firstResult),
							Flowable.range(2, pages - 1)
									.parallel(4)
									.runOn(VirtualThreads.SCHEDULER)
									.flatMap(page -> Flowable.fromIterable(decodeResponse(fetcher.apply(page))))
									.sequential());
				})
				.compose(Rx3Util.retryWithDelayFlowable(2, Duration.ofSeconds(1)));
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
	public Flowable<JsonNode> fetchPagesOfJsonArrays(
			EsiUrl url, BiFunction<JsonNode, Response, JsonNode> augmenter, Optional<String> accessToken) {
		var responses = fetchPages(url, accessToken);
		return Flowable.fromIterable(responses)
				.compose(standardErrorHandling(url))
				.flatMap(response -> {
					var node = decodeResponse(response);
					return decodeArrayNode(url, node).map(entry -> augmenter.apply(entry, response));
				})
				.doFinally(() -> {
					for (var response : responses) {
						response.close();
					}
				});
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
	 * Dismantles the elements from an json array.
	 * @param url
	 * @param node
	 * @return
	 */
	public Flowable<JsonNode> decodeArrayNode(EsiUrl url, JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			log.warn("Empty response from {}", url);
			return Flowable.empty();
		}
		if (!node.isArray()) {
			return Flowable.error(
					new RuntimeException(String.format("Expected array, got %s - %s", node.getNodeType(), url)));
		}
		return Flowable.fromIterable(node);
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

	public StandardErrorTransformer standardErrorHandling(EsiUrl url) {
		return new StandardErrorTransformer(url);
	}
}
