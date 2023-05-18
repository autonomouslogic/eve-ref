package com.autonomouslogic.everef.http;

import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Trace logs calls
 */
@Singleton
@Log4j2
public class LoggingInterceptor implements Interceptor {
	@Inject
	protected LoggingInterceptor() {}

	@NotNull
	@Override
	public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
		var req = chain.request();
		log.trace("{} {}", req.method(), req.url());
		final var response = chain.proceed(chain.request());
		final var networkResponse = response.networkResponse();
		final var cacheResponse = response.cacheResponse();
		var cached = cacheResponse != null && networkResponse == null;
		var code = Optional.ofNullable(networkResponse)
				.or(() -> Optional.ofNullable(response))
				.map(Response::code)
				.orElse(-1);
		var time = Optional.ofNullable(networkResponse)
				.map(r -> r.receivedResponseAtMillis() - r.sentRequestAtMillis())
				.orElse(-1L);
		var cacheOrTime = cached ? "cached" : time + " ms";
		log.trace("{} {} -> {} [{}]", req.method(), req.url(), code, cacheOrTime);
		return response;
	}
}
