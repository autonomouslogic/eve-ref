package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Adds a user agent header.
 */
@Singleton
@Log4j2
public class EsiUserAgentInterceptor implements Interceptor {
	private final String userAgent;

	@Inject
	protected EsiUserAgentInterceptor() {
		userAgent = Configs.ESI_USER_AGENT.getRequired();
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
		var request = chain.request().newBuilder();
		request.addHeader("User-Agent", userAgent);
		return chain.proceed(request.build());
	}
}
