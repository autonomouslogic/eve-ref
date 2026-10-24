package com.autonomouslogic.everef.http;

import java.io.IOException;
import java.net.URI;
import lombok.NonNull;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Refuses any request outside of a given host and base path, so a misconfigured or hardcoded URL can't
 * silently hit the wrong site, or a wrong path on the right site, through the client it's attached to.
 */
public class BasePathInterceptor implements Interceptor {
	private final String host;
	private final String basePath;

	public BasePathInterceptor(@NonNull URI baseUrl) {
		this.host = baseUrl.getHost();
		var path = baseUrl.getPath() == null ? "" : baseUrl.getPath();
		this.basePath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
		var request = chain.request();
		var url = request.url();
		var requestHost = url.host();
		var requestPath = url.encodedPath();
		var withinBasePath = requestPath.equals(basePath) || requestPath.startsWith(basePath + "/");
		if (!requestHost.equalsIgnoreCase(host) || !withinBasePath) {
			throw new IOException(String.format(
					"Refusing request to '%s%s': client is restricted to '%s%s/'",
					requestHost, requestPath, host, basePath));
		}
		return chain.proceed(request);
	}
}
