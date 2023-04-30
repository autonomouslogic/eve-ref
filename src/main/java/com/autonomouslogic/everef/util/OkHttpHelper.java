package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Singleton
@Log4j2
public class OkHttpHelper {
	@Inject
	protected OkHttpHelper() {}

	public Single<Response> get(@NonNull String url, @NonNull OkHttpClient client) {
		return get(url, client, Schedulers.io());
	}

	public Single<Response> get(@NonNull String url, @NonNull OkHttpClient client, @NonNull Scheduler scheduler) {
		return get(url, client, scheduler, 2, true);
	}

	private Single<Response> get(
			@NonNull String url,
			@NonNull OkHttpClient client,
			@NonNull Scheduler scheduler,
			int retries,
			boolean observeOnComputation) {
		return Single.defer(() -> {
			var request = getRequest(url);
			var response = execute(request, client, scheduler);
			if (retries > 0) {
				response = response.retry(retries, e -> {
					var msg = ExceptionUtils.getMessage(e);
					log.warn(String.format("Retrying request: %s %s - %s", request.method(), request.url(), msg));
					return true;
				});
			}
			response = response.onErrorResumeNext(
					e -> Single.error(new RuntimeException(String.format("Error during GET %s", url), e)));
			if (observeOnComputation) {
				response = response.observeOn(Schedulers.computation());
			}
			return response;
		});
	}

	public Single<Response> download(@NonNull String url, @NonNull File file, @NonNull OkHttpClient client) {
		return download(url, file, client, Schedulers.io());
	}

	public Single<Response> download(
			@NonNull String url, @NonNull File file, @NonNull OkHttpClient client, @NonNull Scheduler scheduler) {
		return Single.defer(() -> {
					log.debug("Downloading {} to {}", url, file);
					return get(url, client, scheduler, 0, false)
							.flatMap(response -> Single.fromCallable(() -> {
								var lastModified = getLastModified(response);
								if (response.code() != 200) {
									return response;
								}
								try (var in = response.body().byteStream();
										var out = new FileOutputStream(file)) {
									IOUtils.copy(in, out);
								}
								if (lastModified.isPresent()) {
									Files.setLastModifiedTime(
											file.toPath(),
											FileTime.from(lastModified.get().toInstant()));
								}
								return response;
							}));
				})
				.observeOn(Schedulers.computation());
	}

	/**
	 * Executes a request on the provided scheduler.
	 * Not that this does not bring execution back to the computation scheduler.
	 * @param request
	 * @param client
	 * @param scheduler
	 * @return
	 */
	private Single<Response> execute(
			@NonNull Request request, @NonNull OkHttpClient client, @NonNull Scheduler scheduler) {
		return Single.fromCallable(() -> {
					log.trace(String.format("Requesting %s %s", request.method(), request.url()));
					return client.newCall(request).execute();
				})
				.subscribeOn(scheduler)
				.onErrorResumeNext(e -> Single.error(new RuntimeException(
						String.format("Error requesting %s %s", request.method(), request.url()), e)));
	}

	private Request getRequest(@NonNull String url) {
		return new Request.Builder().get().url(url).build();
	}

	public Optional<ZonedDateTime> getLastModified(@NonNull Response response) {
		var lastModifiedHeader = response.header("Last-Modified");
		if (lastModifiedHeader == null) {
			return Optional.empty();
		}
		var lastModified = ZonedDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME);
		return Optional.of(lastModified);
	}
}
