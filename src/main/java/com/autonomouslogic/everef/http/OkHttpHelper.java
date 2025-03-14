package com.autonomouslogic.everef.http;

import com.autonomouslogic.everef.util.VirtualThreads;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

@Singleton
@Log4j2
public class OkHttpHelper {
	@Inject
	protected OkHttpHelper() {}

	public Single<Response> get(
			@NonNull String url, @NonNull OkHttpClient client, @NonNull Consumer<Request.Builder> requestConsumer) {
		return get(url, client, 2, requestConsumer);
	}

	public Single<Response> get(@NonNull String url, @NonNull OkHttpClient client) {
		return get(url, client, 2, r -> {});
	}

	private Single<Response> get(
			@NonNull String url,
			@NonNull OkHttpClient client,
			int retries,
			@NonNull Consumer<Request.Builder> requestConsumer) {
		return Single.defer(() -> {
			var request = getRequest(url, requestConsumer);
			return executeWithRetries(url, client, retries, request);
		});
	}

	private @NotNull Single<Response> executeWithRetries(
			@NotNull String url, @NotNull OkHttpClient client, int retries, Request request) {
		var response = execute(request, client);
		if (retries > 0) {
			response = response.retry(retries, e -> {
				var msg = ExceptionUtils.getMessage(e);
				log.warn(String.format("Retrying request: %s %s - %s", request.method(), request.url(), msg));
				return true;
			});
		}
		response = response.onErrorResumeNext(
				e -> Single.error(new RuntimeException(String.format("Error during %s %s", request.method(), url), e)));
		return response;
	}

	public Single<Response> download(@NonNull String url, @NonNull File file, @NonNull OkHttpClient client) {
		return Single.defer(() -> {
					log.debug("Downloading {} to {}", url, file);
					return get(url, client, 0, r -> {})
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
				.observeOn(VirtualThreads.SCHEDULER);
	}

	public Single<Response> post(
			@NonNull String url,
			@NonNull byte[] body,
			@NonNull OkHttpClient client,
			@NonNull Consumer<Request.Builder> requestConsumer) {
		return post(url, body, client, 0, requestConsumer);
	}

	private Single<Response> post(
			@NonNull String url,
			@NonNull byte[] body,
			@NonNull OkHttpClient client,
			int retries,
			@NonNull Consumer<Request.Builder> requestConsumer) {
		return Single.defer(() -> {
			var request = postRequest(url, body, requestConsumer);
			return executeWithRetries(url, client, retries, request);
		});
	}

	/**
	 * Executes a request on the provided scheduler.
	 * Not that this does not bring execution back to the computation scheduler.
	 * @param request
	 * @param client
	 * @return
	 */
	public Single<Response> execute(@NonNull Request request, @NonNull OkHttpClient client) {
		return Single.fromCallable(() -> {
					log.trace(String.format("Requesting %s %s", request.method(), request.url()));
					return client.newCall(request).execute();
				})
				.subscribeOn(Schedulers.io())
				.observeOn(VirtualThreads.SCHEDULER)
				.onErrorResumeNext(e -> Single.error(new RuntimeException(
						String.format("Error requesting %s %s", request.method(), request.url()), e)));
	}

	public Request getRequest(@NonNull String url) {
		return getRequest(url, b -> {});
	}

	public Request getRequest(@NonNull String url, @NonNull Consumer<Request.Builder> requestConsumer) {
		var builder = new Request.Builder().get().url(url);
		requestConsumer.accept(builder);
		return builder.build();
	}

	public Request postRequest(
			@NonNull String url, @NonNull byte[] body, @NonNull Consumer<Request.Builder> requestConsumer) {
		var builder = new Request.Builder().post(RequestBody.create(body)).url(url);
		requestConsumer.accept(builder);
		return builder.build();
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
