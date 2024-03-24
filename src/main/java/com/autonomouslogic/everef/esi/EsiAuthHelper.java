package com.autonomouslogic.everef.esi;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.dynamomapper.DynamoAsyncMapper;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.CharacterLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
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
			List.of("esi-universe.read_structures.v1", "esi-markets.structure_markets.v1");
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

	@Inject
	protected DynamoAsyncMapper dynamoAsyncMapper;

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
	public Single<OAuth2AccessToken> getAccessToken(@NonNull String code) {
		return Single.fromFuture(service.getAccessTokenAsync(code))
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation());
	}

	@SneakyThrows
	public Single<OAuth2AccessToken> refreshAccessToken(@NonNull String refreshToken) {
		return Single.fromFuture(service.refreshAccessTokenAsync(refreshToken))
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation());
	}

	@SneakyThrows
	public Single<EsiVerifyResponse> verify(@NonNull String token) {
		return Single.defer(() -> {
					var url = new URL(Configs.ESI_BASE_URL.getRequired().toURL(), "/verify/");
					var request = okHttpHelper
							.getRequest(url.toString())
							.newBuilder()
							.header("Authorization", "Bearer " + token)
							.build();
					return okHttpHelper.execute(request, esiHttpClient, Schedulers.io());
				})
				.map(response -> {
					var verify = objectMapper.readValue(response.body().byteStream(), EsiVerifyResponse.class);
					response.close();
					return verify;
				});
	}

	@SneakyThrows
	public Completable putCharacterLogin(CharacterLogin characterLogin) {
		return Completable.defer(() -> Rx3Util.toSingle(dynamoAsyncMapper.putItemFromKeyObject(characterLogin))
				.ignoreElement());
	}

	@SneakyThrows
	public Maybe<CharacterLogin> getCharacterLogin(String ownerHash) {
		return Rx3Util.toMaybe(dynamoAsyncMapper.getItemFromPrimaryKey(ownerHash, CharacterLogin.class))
				.flatMap(r -> Maybe.fromOptional(Optional.ofNullable(r.item())));
	}
}
