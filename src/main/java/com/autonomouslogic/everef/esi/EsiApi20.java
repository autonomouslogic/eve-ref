package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.github.scribejava.core.builder.api.DefaultApi20;
import lombok.Getter;

public class EsiApi20 extends DefaultApi20 {
	@Getter
	private final String authorizationBaseUrl =
			Configs.EVE_OAUTH_AUTHORIZATION_URL.getRequired().toString();

	@Getter
	private final String accessTokenEndpoint =
			Configs.EVE_OAUTH_TOKEN_URL.getRequired().toString();
}
