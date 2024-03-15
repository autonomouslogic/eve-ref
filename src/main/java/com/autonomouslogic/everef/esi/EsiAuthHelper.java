package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.net.URI;
import java.net.URL;
import java.util.List;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * @link <a href="https://www.pac4j.org/docs/clients/openid-connect.html">pac4j OpenID Connect</a>
 */
public class EsiAuthHelper {
	private static final List<String> scopes =
			List.of("esi-markets.structure_markets.v1", "esi-universe.read_structures.v1");
	private static final URL callbackUrl = Configs.OAUTH_CALLBACK_URL.getRequired();

	private final OAuth20Service service;

	@Inject
	protected EsiAuthHelper() {
		var clientId = Configs.EVE_OAUTH_CLIENT_ID.getRequired();
		var secretKey = Configs.EVE_OAUTH_SECRET_KEY.getRequired();

		service = new ServiceBuilder(clientId)
				.apiSecret(secretKey)
				.defaultScope(String.join(" ", scopes))
				.callback(callbackUrl.toString())
				.build(new EsiApi20());
	}

	@SneakyThrows
	public URI getLoginUri() {
		return new URI(service.getAuthorizationUrl("no-state"));
	}

	@SneakyThrows
	public OAuth2AccessToken getAccessToken(@NonNull String code) {
		return service.getAccessToken(code);
	}

	@SneakyThrows
	public OAuth2AccessToken refreshAccessToken(@NonNull String refreshToken) {
		return service.refreshAccessToken(refreshToken);
	}
}
