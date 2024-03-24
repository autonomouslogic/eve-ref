package com.autonomouslogic.everef.cli.basiclogin;

import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.model.CharacterLogin;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import javax.ws.rs.QueryParam;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Controller
@Path("/")
@Log4j2
public class BasicLoginController {
	@Inject
	protected EsiAuthHelper esiAuthHelper;

	@GET
	@Produces("text/html")
	@SneakyThrows
	public String index() {
		return """
			<h1>EVE Ref Login</h1>
			<a href="/basic-login">Login</a>
			""";
	}

	@GET
	@Path("/basic-login")
	@SneakyThrows
	public HttpResponse<String> loginRedirect() {
		return HttpResponse.redirect(esiAuthHelper.getLoginUri());
	}

	@GET
	@Path("/basic-login-callback")
	@Produces("text/html")
	public String callback(@QueryParam("code") String code, @QueryParam("state") String state) {
		var token = esiAuthHelper.getAccessToken(code);
		var verify = esiAuthHelper.verify(token.getAccessToken());

		var characterLogin = CharacterLogin.builder()
				.characterId(verify.getCharacterId())
				.characterName(verify.getCharacterName())
				.characterOwnerHash(verify.getCharacterOwnerHash())
				.refreshToken(token.getRefreshToken())
				.scopes(verify.getScopes())
				.build();
		esiAuthHelper.putCharacterLogin(characterLogin);

		return String.format(
				"""
				<h1>EVE Ref Login</h1>
				<h2>OAuth2</h2>
				<ul>
					<li>accessToken: <code>%s</code></li>
					<li>tokenType: <code>%s</code></li>
					<li>expiresIn: <code>%s</code></li>
					<li>refreshToken: <code>%s</code></li>
					<li>scope: <code>%s</code></li>
				</ul>
				<h2>EVE Online</h2>
				<ul>
					<li>characterId: <code>%s</code></li>
					<li>characterName: <code>%s</code></li>
					<li>characterOwnerHash: <code>%s</code></li>
					<li>expiresOn: <code>%s</code></li>
					<li>scopes: <code>%s</code></li>
				</ul>
				""",
				token.getAccessToken(),
				token.getTokenType(),
				token.getExpiresIn(),
				token.getRefreshToken(),
				token.getScope(),
				verify.getCharacterId(),
				verify.getCharacterName(),
				verify.getCharacterOwnerHash(),
				verify.getExpiresOn(),
				verify.getScopes());
	}
}
