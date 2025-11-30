package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.config.Configs;
import io.helidon.webserver.http.ServerResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;

@Singleton
public class ApiResponseUtil {
	private static final String CACHE_CONTROL_HEADER = String.format(
			"public, max-age=%d, immutable", Duration.ofMinutes(10).toSeconds());

	private final String eveRefVersion;

	@Inject
	public ApiResponseUtil() {
		this.eveRefVersion = Configs.EVE_REF_VERSION.getRequired();
	}

	public void setStandardHeaders(ServerResponse res) {
		setStandardHeaders(res, null);
	}

	public void setStandardHeaders(ServerResponse res, String docsUrl) {
		res.header("Server", "eve-ref/" + eveRefVersion)
				.header("Content-Type", "application/json")
				.header("X-Discord", "https://everef.net/discord")
				.header("X-OpenAPI", "https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml")
				.header("Cache-Control", CACHE_CONTROL_HEADER);
		if (docsUrl != null) {
			res.header("X-Docs", docsUrl);
		}
	}
}
