package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.search.SearchResult;
import com.autonomouslogic.everef.service.search.SearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Tag(name = "search")
@Path("/v1/search")
@Log4j2
public class SearchHandler implements HttpService, Handler {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected SearchService searchService;

	@Inject
	protected ApiUtil apiUtil;

	@Inject
	public SearchHandler() {}

	@GET
	@Operation(summary = "Search for inventory types", description = "Search for EVE Online inventory types by name")
	@ApiResponse(
			responseCode = "200",
			description = "Success",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = SearchResult.class)))
	@ApiResponse(
			responseCode = "400",
			description = "Client error",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
	@ApiResponse(
			responseCode = "500",
			description = "Server error",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
	public SearchResult search(
			@Parameter(
							in = ParameterIn.QUERY,
							name = "q",
							description = "Search query (minimum 3 characters)",
							required = true,
							schema = @Schema(type = "string"))
					String q) {
		return searchService.search(q);
	}

	@Override
	public void routing(HttpRules rules) {
		rules.get(this);
	}

	@Override
	@SneakyThrows
	public void handle(ServerRequest req, ServerResponse res) {
		log.info(
				"Received request: {}",
				URLEncoder.encode(req.requestedUri().toUri().toString(), StandardCharsets.UTF_8));
		var q = req.query().first("q").orElse(null);
		SearchResult result;
		try {
			result = search(q);
		} catch (IllegalArgumentException e) {
			throw new ClientException(e.getMessage());
		}
		var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n";
		res.status(Status.OK_200);
		apiUtil.setStandardHeaders(res, Duration.ofMinutes(10));
		res.send(json);
	}
}
