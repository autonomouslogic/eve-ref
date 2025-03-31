package com.autonomouslogic.everef.http;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;

@RequiredArgsConstructor
@Log4j2
public class OkHttpWrapper {
	private final OkHttpClient client;

	public Response get(@NonNull String url) {
		return get(url, r -> {});
	}

	public Response get(@NonNull String url, @NonNull Consumer<Request.Builder> requestConsumer) {
		var request = getRequest(url, requestConsumer);
		return execute(request);
	}

	@SneakyThrows
	public Response download(@NonNull String url, @NonNull File file) {
		log.debug("Downloading {} to {}", url, file);
		try (var response = get(url)) {
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
						file.toPath(), FileTime.from(lastModified.get().toInstant()));
			}
			return response;
		}
	}

	public Response post(
			@NonNull String url, @NonNull byte[] body, @NonNull Consumer<Request.Builder> requestConsumer) {
		var request = postRequest(url, body, requestConsumer);
		return execute(request);
	}

	/**
	 * Executes a request on the provided scheduler.
	 * Not that this does not bring execution back to the computation scheduler.
	 * @param request
	 * @param client
	 * @return
	 */
	public Response execute(@NonNull Request request) {
		try {
			log.trace(String.format("Requesting %s %s", request.method(), request.url()));
			return client.newCall(request).execute();
		} catch (Exception e) {
			throw new RuntimeException(String.format("Error requesting %s %s", request.method(), request.url()), e);
		}
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
