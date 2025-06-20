package com.autonomouslogic.everef.esi;

import com.autonomouslogic.dynamomapper.DynamoAsyncMapper;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.model.CharacterLogin;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.reactivex.rxjava3.core.Completable;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @link <a href="https://www.pac4j.org/docs/clients/openid-connect.html">pac4j OpenID Connect</a>
 */
@Log4j2
public class EsiAuthHelper {
	private static final Duration EXPIRATION_BUFFER = Duration.ofMinutes(1);
	private static final List<String> SCOPES = List.of(
			"esi-universe.read_structures.v1",
			"esi-markets.structure_markets.v1",
			"esi-wallet.read_character_wallet.v1",
			"esi-wallet.read_corporation_wallet.v1",
			"esi-wallet.read_corporation_wallets.v1");
	private static final URL CALLBACK_URL = Configs.OAUTH_CALLBACK_URL.getRequired();

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	@Named("esi")
	protected OkHttpWrapper esiHttpWrapper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected DynamoAsyncMapper dynamoAsyncMapper;

	private final Cache<String, Pair<OAuth2AccessToken, Instant>> tokenCache =
			CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(1)).build();

	private final OAuth20Service service;

	@Inject
	protected EsiAuthHelper() {
		var clientId = Configs.EVE_OAUTH_CLIENT_ID.getRequired();
		var secretKey = Configs.EVE_OAUTH_SECRET_KEY.getRequired();

		service = new ServiceBuilder(clientId)
				.apiSecret(secretKey)
				.defaultScope(String.join(" ", SCOPES))
				.callback(CALLBACK_URL.toString())
				.build(new EsiApi20());
	}

	@SneakyThrows
	public URI getLoginUri() {
		var state = new byte[128 / 8];
		new SecureRandom().nextBytes(state);
		return new URI(service.getAuthorizationUrl(Hex.encodeHexString(state)));
	}

	@SneakyThrows
	public OAuth2AccessToken getAccessToken(@NonNull String code) {
		return service.getAccessTokenAsync(code).get();
	}

	@SneakyThrows
	public OAuth2AccessToken refreshAccessToken(@NonNull String refreshToken) {
		return service.refreshAccessTokenAsync(refreshToken).get();
	}

	@SneakyThrows
	public EsiVerifyResponse verify(@NonNull String token) {
		var url = new URL(Configs.ESI_BASE_URL.getRequired().toURL(), "/verify/");
		var request = esiHttpWrapper
				.getRequest(url.toString())
				.newBuilder()
				.header("Authorization", "Bearer " + token)
				.build();
		try (var response = esiHttpWrapper.execute(request)) {
			var verify = objectMapper.readValue(response.body().byteStream(), EsiVerifyResponse.class);
			return verify;
		}
	}

	@SneakyThrows
	public Completable putCharacterLogin(CharacterLogin characterLogin) {
		return Completable.defer(() -> Rx.toSingle(dynamoAsyncMapper.putItemFromKeyObject(characterLogin))
				.observeOn(VirtualThreads.SCHEDULER)
				.ignoreElement());
	}

	@SneakyThrows
	public Optional<CharacterLogin> getCharacterLogin(String ownerHash) {
		return Optional.ofNullable(dynamoAsyncMapper
				.getItemFromPrimaryKey(ownerHash, CharacterLogin.class)
				.join()
				.item());
	}

	public Optional<OAuth2AccessToken> getTokenForOwnerHash(String ownerHash) {
		var cached = tokenCache.getIfPresent(ownerHash);
		if (cached != null) {
			var issued = cached.getRight();
			var expiresIn = cached.getLeft().getExpiresIn();
			var expiration = issued.plusSeconds(expiresIn).minus(EXPIRATION_BUFFER);
			if (Instant.now().isBefore(expiration)) {
				return Optional.of(cached.getLeft());
			}
		}
		log.debug("Refreshing token for ownerHash {}", ownerHash);
		var token = getCharacterLogin(ownerHash).map(login -> refreshAccessToken(login.getRefreshToken()));
		token.ifPresent(t -> tokenCache.put(ownerHash, Pair.of(t, Instant.now())));
		return token;
	}

	public String getTokenStringForOwnerHash(String ownerHash) {
		return getTokenForOwnerHash(ownerHash)
				.map(token -> token.getAccessToken())
				.orElseThrow(
						() -> new RuntimeException(String.format("Login not found for owner hash: %s", ownerHash)));
	}
}
