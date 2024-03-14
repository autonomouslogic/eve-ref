package com.autonomouslogic.everef.cli.basiclogin;

import com.autonomouslogic.everef.config.Configs;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Produces;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringEncoder;
import lombok.extern.log4j.Log4j2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Path("/")
@Log4j2
public class BasicLoginController {
	private static final List<String> scopes = List.of(
		"esi-markets.structure_markets.v1",
		"esi-universe.read_structures.v1"
	);

	@GET
	@Produces("text/html")
	public String index() {
		// Prepare EVE login URL.
		QueryStringEncoder location = new QueryStringEncoder(Configs.EVE_OAUTH_AUTHORIZE_URL.getRequired().toString());
		location.addParam("response_type", "code");
		location.addParam("redirect_uri", "http://localhost:" + Configs.MICRONAUT_PORT.getRequired() + "/basic-login-callback");
		location.addParam("client_id", Configs.EVE_OAUTH_SECRET_KEY.getRequired());
		location.addParam("scope", scopes.stream().collect(Collectors.joining(" ")));
		location.addParam("state", "local");
		// Prepare headers.
		HttpHeaders headers = new DefaultHttpHeaders();
		headers.add(HttpHeaderNames.LOCATION, location.toString());
		// Send redirect.
		responder.sendStatus(
			HttpResponseStatus.FOUND,
			headers
		);
	}

	@GET
	@Path("/basic-login-callback")
	@Produces("text/html")
	public String callback() {
		return "Hello, world!";
	}
}
