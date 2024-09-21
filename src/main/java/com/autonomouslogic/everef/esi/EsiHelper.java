package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.openapi.esi.infrastructure.ApiResponse;
import com.autonomouslogic.everef.openapi.esi.infrastructure.ResponseType;
import com.autonomouslogic.everef.openapi.esi.infrastructure.Success;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Response;

@Singleton
@Log4j2
public class EsiHelper {
	private static final String PAGES_HEADER = "X-Pages";
	public static final Scheduler ESI_SCHEDULER;

	static {
		var threads = Configs.ESI_HTTP_THREADS.getRequired();
		var factory = new ThreadFactoryBuilder().setNameFormat("esi-http-%d").build();
		log.debug("Using {} threads for ESI HTTP requests", threads);
		ESI_SCHEDULER = Schedulers.from(Executors.newFixedThreadPool(threads, factory));
	}

	@Inject
	@Named("esi")
	protected OkHttpClient esiHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

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
	public Single<Response> fetch(EsiUrl url) {
		return fetch(url, Optional.empty());
	}

	/**
	 * Fetches the requested URL.
	 * This call does NOT include standard error handling.
	 * @param url
	 * @return
	 */
	public Single<Response> fetch(EsiUrl url, Optional<String> accessToken) {
		return okHttpHelper.get(
				url.toString(),
				esiHttpClient,
				ESI_SCHEDULER,
				r -> accessToken.ifPresent(token -> r.addHeader("Authorization", "Bearer " + token)));
	}

	/**
	 * Fetches the requested URL.
	 * This call does NOT include standard error handling.
	 * @param url
	 * @return
	 */
	public Single<Response> fetch(EsiUrl url, Maybe<String> accessToken) {
		return accessToken
				.map(Optional::of)
				.switchIfEmpty(Single.just(Optional.empty()))
				.flatMap(token -> fetch(url, token));
	}

	/**
	 * Fetches the requested URL and automatically fetches all subsequent pages indicated by the header.
	 * This call does NOT include standard error handling.
	 * @param url
	 * @return
	 */
	protected Flowable<Response> fetchPages(EsiUrl url, Maybe<String> accessToken) {
		return fetch(url, accessToken).flatMapPublisher(first -> {
			var pages = first.header(PAGES_HEADER);
			if (pages == null || pages.isEmpty()) {
				return Flowable.just(first);
			}
			var pagesInt = Integer.parseInt(pages);
			if (pagesInt == 1) {
				return Flowable.just(first);
			}
			return Flowable.concatArray(
					Flowable.just(first),
					Flowable.range(2, pagesInt - 1)
							.flatMapSingle(
									page -> fetch(url.toBuilder().page(page).build(), accessToken), false, 4));
		});
	}

	protected Flowable<Response> fetchPages(EsiUrl url) {
		return fetchPages(url, Maybe.empty());
	}

	/**
	 * Like {@link #fetchPages(EsiUrl)}, but works on the OpenAPI generated ESI API classes.
	 * @param fetcher
	 * @return
	 * @param <T>
	 */
	public <T> Flowable<T> fetchPages(Function<Integer, ApiResponse<List<T>>> fetcher) {
		return Flowable.defer(() -> Flowable.just(fetcher.apply(1))).flatMap(first -> {
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
							.flatMap(page -> Flowable.fromIterable(decodeResponse(fetcher.apply(page))), false, 4));
		})
		// this was causing exceptions to escape to RxJava's main bit and being logged as uncaught
		// .compose(Rx3Util.retryWithDelayFlowable(2, Duration.ofSeconds(1)))
		;
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
			EsiUrl url, BiFunction<JsonNode, Response, JsonNode> augmenter, Maybe<String> accessToken) {
		return fetchPages(url, accessToken).compose(standardErrorHandling(url)).flatMap(response -> {
			var node = decodeResponse(response);
			return decodeArrayNode(url, node).map(entry -> augmenter.apply(entry, response));
		});
	}

	public Flowable<JsonNode> fetchPagesOfJsonArrays(EsiUrl url, BiFunction<JsonNode, Response, JsonNode> augmenter) {
		return fetchPagesOfJsonArrays(url, augmenter, Maybe.empty());
	}

	/**
	 * Decodes a JsonNode from a generic response body.
	 * @param response
	 * @return
	 */
	@SneakyThrows
	public JsonNode decodeResponse(Response response) {
		if (response.code() == 204 || response.code() == 404) {
			return NullNode.getInstance();
		}
		if (response.code() != 200) {
			throw new RuntimeException(String.format("Cannot decode non-200 response: %s", response.code()));
		}
		return objectMapper.readTree(response.peekBody(Long.MAX_VALUE).byteStream());
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
		if (response.getResponseType() != ResponseType.Success) {
			throw new RuntimeException(
					String.format("Unexpected response type: %s - %s", response.getResponseType(), response));
		}
		return ((Success<T>) response).getData();
	}

	/**
	 * Populates the JSON entry with the Last-Modified header from the response.
	 * The timestamp stored is ISO-8601.
	 * @param entry
	 * @param response
	 * @return
	 */
	public JsonNode populateLastModified(JsonNode entry, Response response) {
		var lastModified = okHttpHelper.getLastModified(response);
		var obj = (ObjectNode) entry;
		lastModified.ifPresent(
				date -> obj.put("http_last_modified", date.toInstant().toString()));
		return obj;
	}

	public StandardErrorTransformer standardErrorHandling(EsiUrl url) {
		return new StandardErrorTransformer(url);
	}
}
