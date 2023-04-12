package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.schedulers.Schedulers;
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

	public Single<Response> fetch(EsiUrl url) {
		return okHttpHelper.get(url.toString(), esiHttpClient, ESI_SCHEDULER);
	}

	public Flowable<Response> fetchPages(EsiUrl url) {
		return fetch(url).flatMapPublisher(first -> {
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
									page -> fetch(url.toBuilder().page(page).build()), false, 4));
		});
	}

	/**
	 * Fetches multiple pages of a URL, decoding each page as a JSON array, and returning the objects in a stream.
	 * @param url
	 * @return
	 */
	public Flowable<JsonNode> fetchPagesOfJsonArrays(EsiUrl url) {
		return fetchPagesOfJsonArrays(url, (a, b) -> a);
	}

	public Flowable<JsonNode> fetchPagesOfJsonArrays(EsiUrl url, BiFunction<JsonNode, Response, JsonNode> augmenter) {
		return fetchPages(url).flatMap(response -> {
			var node = decode(response);
			if (node.isMissingNode()) {
				log.warn("Empty response from {}", url);
				return Flowable.empty();
			}
			if (!node.isArray()) {
				return Flowable.error(
						new RuntimeException(String.format("Expected array, got %s - %s", node.getNodeType(), url)));
			}
			return Flowable.fromIterable(node).map(entry -> augmenter.apply(entry, response));
		});
	}

	@SneakyThrows
	public JsonNode decode(Response response) {
		if (response.code() != 200) {
			throw new RuntimeException(String.format("Cannot decode non-200 response: %s", response.code()));
		}
		return objectMapper.readTree(response.peekBody(Long.MAX_VALUE).byteStream());
	}
}
