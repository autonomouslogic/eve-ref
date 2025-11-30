package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.config.Configs;
import io.helidon.webserver.http.ServerResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;

@Singleton
public class ApiResponseUtil {
	private final String eveRefVersion;

	@Inject
	public ApiResponseUtil() {
		this.eveRefVersion = Configs.EVE_REF_VERSION.getRequired();
	}

	public void setStandardHeaders(ServerResponse res, Duration cacheDuration) {
		setStandardHeaders(res, cacheDuration, null);
	}

	public void setStandardHeaders(ServerResponse res, Duration cacheDuration, String docsUrl) {
		String cacheControlHeader = String.format(
				"public, max-age=%d, immutable", cacheDuration.toSeconds());
		res.header("Server", "eve-ref/" + eveRefVersion)
				.header("Content-Type", "application/json")
				.header("X-Discord", "https://everef.net/discord")
				.header("X-OpenAPI", "https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml")
				.header("Cache-Control", cacheControlHeader);
		if (docsUrl != null) {
			res.header("X-Docs", docsUrl);
		}
	}
}
