package com.autonomouslogic.everef.cli.basiclogin;

import com.autonomouslogic.everef.esi.EsiAuthHelper;
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
		return "<a href=\"/basic-login\">Login</a>";
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
		return String.format(
				"""
				<ul>
					<li>accessToken: <code>%s</code></li>
					<li>tokenType: <code>%s</code></li>
					<li>expiresIn: <code>%s</code></li>
					<li>refreshToken: <code>%s</code></li>
					<li>scope: <code>%s</code></li>
				</ul>
			""",
				token.getAccessToken(),
				token.getTokenType(),
				token.getExpiresIn(),
				token.getRefreshToken(),
				token.getScope());
	}
}
