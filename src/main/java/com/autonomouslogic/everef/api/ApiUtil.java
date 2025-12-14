package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.config.Configs;
import io.helidon.webserver.http.ServerResponse;
import java.time.Duration;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Path;

@Singleton
public class ApiUtil {
	private final String eveRefVersion;

	@Inject
	public ApiUtil() {
		this.eveRefVersion = Configs.EVE_REF_VERSION.getRequired();
	}

	public void setStandardHeaders(ServerResponse res, Duration cacheDuration) {
		setStandardHeaders(res, cacheDuration, null);
	}

	public void setStandardHeaders(ServerResponse res, Duration cacheDuration, String docsUrl) {
		String cacheControlHeader = String.format("public, max-age=%d, immutable", cacheDuration.toSeconds());
		res.header("Server", "eve-ref/" + eveRefVersion)
				.header("Content-Type", "application/json")
				.header("X-Discord", "https://everef.net/discord")
				.header("X-OpenAPI", "https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml")
				.header("Cache-Control", cacheControlHeader);
		if (docsUrl != null) {
			res.header("X-Docs", docsUrl);
		}
	}

	public String getApiPath(Class<?> clazz) {
		return Objects.requireNonNull(clazz.getAnnotation(Path.class), clazz + " is missing @Path annotation")
				.value();
	}
}
