package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
@Log4j2
public class OkHttpHelper {
	@Inject
	protected OkHttpHelper() {}

	public Single<Response> get(String url, OkHttpClient client) {
		return get(url, client, Schedulers.io());
	}

	public Single<Response> get(String url, OkHttpClient client, Scheduler scheduler) {
		return Single.defer(() -> {
			var request = getRequest(url);
			return execute(request, client, scheduler)
				.retry(10, e -> {
					log.warn(String.format("Retrying request: %s %s - %s %s", request.method(), request.url(), e.getClass().getSimpleName(), e.getMessage()));
					return true;
				});
		});
	}

	public Single<Response> execute(Request request, OkHttpClient client, Scheduler scheduler) {
		return Single.fromCallable(() -> {
					log.trace(String.format("Requesting %s %s", request.method(), request.url()));
					return client.newCall(request).execute();
				})
				.subscribeOn(scheduler)
				.observeOn(Schedulers.computation())
				.onErrorResumeNext(e -> Single.error(new RuntimeException(
						String.format("Error requesting %s %s", request.method(), request.url()), e)));
	}

	private Request getRequest(String url) {
		return new Request.Builder().get().url(url).build();
	}

	public Optional<ZonedDateTime> getLastModified(Response response) {
		var lastModifiedHeader = response.header("Last-Modified");
		if (lastModifiedHeader == null) {
			return Optional.empty();
		}
		var lastModified = ZonedDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME);
		return Optional.of(lastModified);
	}
}
