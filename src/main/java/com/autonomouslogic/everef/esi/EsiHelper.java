package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Supplier;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Singleton
@Log4j2
public class EsiHelper {
	private static final String PAGES_HEADER = "X-Pages";

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
		return okHttpHelper.get(url.toString(), esiHttpClient);
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
				Flowable.range(2, pagesInt)
					.flatMapSingle(page -> fetch(url), false, 1)
			);
		});
	}

	/**
	 * Fetches multiple pages of a URL, decoding each page as a JSON array, and returning the objects in a stream.
	 * @param url
	 * @return
	 */
	public Flowable<JsonNode> fetchPagesOfJsonArrays(EsiUrl url) {
		return fetchPages(url).map(this::decode)
			.flatMap(node -> {
				if (!node.isArray()) {
					return Flowable.error(new RuntimeException(String.format("Expected array, got %s", node.getNodeType())));
				}
				return Flowable.fromIterable(node);
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
