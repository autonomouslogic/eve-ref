package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.deeparchive.DeepArchiveSeeders;
import com.autonomouslogic.everef.service.DeepArchiveSeedersService;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.time.Duration;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.json.JsonMapper;

@Tag(name = "deep-archive")
@Path("/v1/deep-archive/seeders")
@Log4j2
public class DeepArchiveSeedersHandler implements HttpService, Handler {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected DeepArchiveSeedersService deepArchiveSeedersService;

	@Inject
	protected ApiUtil apiUtil;

	private final Duration cacheControlMaxAge;

	@Inject
	public DeepArchiveSeedersHandler() {
		cacheControlMaxAge = Configs.DEEP_ARCHIVE_SEEDERS_CACHE_CONTROL_MAX_AGE.getRequired();
	}

	@GET
	@Operation(
			summary = "Get Deep Archive seeder counts",
			description = "Number of seeders for each Deep Archive torrent, as reported by the tracker")
	@ApiResponse(
			responseCode = "200",
			description = "Success",
			content =
					@Content(
							mediaType = "application/json",
							schema = @Schema(implementation = DeepArchiveSeeders.class)))
	@ApiResponse(
			responseCode = "500",
			description = "Server error",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
	public DeepArchiveSeeders getDeepArchiveSeeders() {
		return deepArchiveSeedersService.getSeeders();
	}

	@Override
	public void routing(HttpRules rules) {
		rules.get(this);
	}

	@Override
	@SneakyThrows
	public void handle(ServerRequest req, ServerResponse res) {
		var json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(getDeepArchiveSeeders()) + "\n";
		res.status(Status.OK_200);
		apiUtil.setStandardHeaders(res, cacheControlMaxAge, "https://docs.everef.net/datasets/deep-archive.html");
		res.header("Access-Control-Allow-Origin", "*");
		res.send(json);
	}
}
