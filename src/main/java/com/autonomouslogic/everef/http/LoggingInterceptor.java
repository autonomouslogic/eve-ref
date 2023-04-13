package com.autonomouslogic.everef.http;

import java.io.IOException;
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
		var response = chain.proceed(chain.request());
		var cached = response.cacheResponse() != null;
		var code = response.code();
		var time = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
		var cacheOrTime = cached ? "cached" : time + " ms";
		log.trace("{} {} -> {} - {}", req.method(), req.url(), code, cacheOrTime);
		return response;
	}
}
