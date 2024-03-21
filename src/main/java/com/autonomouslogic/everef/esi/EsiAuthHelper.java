package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.net.URI;
import java.net.URL;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;

/**
 * @link <a href="https://www.pac4j.org/docs/clients/openid-connect.html">pac4j OpenID Connect</a>
 */
public class EsiAuthHelper {
	private static final List<String> scopes =
			List.of("esi-markets.structure_markets.v1", "esi-universe.read_structures.v1");
	private static final URL callbackUrl = Configs.OAUTH_CALLBACK_URL.getRequired();

	@Inject
	@Named("esi")
	protected OkHttpClient esiHttpClient;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected ObjectMapper objectMapper;

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

	@SneakyThrows
	public EsiVerifyResponse verify(@NonNull String token) {
		var url = new URL(Configs.ESI_BASE_URL.getRequired().toURL(), "/verify/");
		var reqeust = okHttpHelper
				.getRequest(url.toString())
				.newBuilder()
				.header("Authorization", "Bearer " + token)
				.build();
		var response =
				okHttpHelper.execute(reqeust, esiHttpClient, Schedulers.io()).blockingGet();
		var verify = objectMapper.readValue(response.body().byteStream(), EsiVerifyResponse.class);
		response.close();
		return verify;
	}
}
