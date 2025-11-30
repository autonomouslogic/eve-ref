package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.search.SearchInventoryType;
import com.autonomouslogic.everef.model.api.search.SearchResponse;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.service.SearchService;
import com.autonomouslogic.everef.util.MarketGroupHelper;
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
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import lombok.extern.log4j.Log4j2;

@Tag(name = "search")
@Path("/v1/search")
@Log4j2
public class SearchHandler implements HttpService, Handler {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected SearchService searchService;

	@Inject
	protected ApiResponseUtil apiResponseUtil;

	@Inject
	public SearchHandler() {}

	@GET
	@Operation(summary = "Search for inventory types", description = "Search for EVE Online inventory types by name")
	@ApiResponse(
			responseCode = "200",
			description = "Success",
			content =
					@Content(
							mediaType = "application/json",
							schema = @Schema(implementation = SearchResponse.class)))
	@ApiResponse(
			responseCode = "400",
			description = "Client error",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
	@ApiResponse(
			responseCode = "500",
			description = "Server error",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
	@Parameter(
			in = ParameterIn.QUERY,
			name = "q",
			description = "Search query (minimum 3 characters)",
			required = false,
			schema = @Schema(type = "string"))
	public SearchResponse search(@Parameter(hidden = true) String q) {
		if (q == null || q.length() < 3) {
			return SearchResponse.builder().input(q != null ? q : "").build();
		}

		List<InventoryType> searchResults = searchService.searchType(q);

		return SearchResponse.builder()
				.input(q)
				.searchInventoryType(searchResults.stream()
						.map(item -> {
							MarketGroup marketGroup =
									MarketGroupHelper.getRootMarketGroup(item, refDataService.getLoadedRefData());
							return SearchInventoryType.builder()
									.typeId(item.getTypeId())
									.nameEn(item.getName().get("en"))
									.rootMarketGroup(Optional.ofNullable(marketGroup)
											.flatMap(g -> Optional.ofNullable(
													g.getName().get("en")))
											.orElse("Inventory type"))
									.rootMarketGroupId(Optional.ofNullable(marketGroup)
											.flatMap(g -> Optional.ofNullable(g.getMarketGroupId()))
											.orElse(null))
									.build();
						})
						.collect(Collectors.toList()))
				.build();
	}

	@Override
	public void routing(HttpRules rules) {
		rules.get(this);
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) {
		String q = req.query().first("q").orElse(null);
		SearchResponse result = this.search(q);
		sendResponse(req, res, result);
	}

	public void sendResponse(ServerRequest req, ServerResponse res, SearchResponse result) {
		try {
			var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n";
			res.status(Status.OK_200);
			apiResponseUtil.setStandardHeaders(res, Duration.ofMinutes(10));
			res.send(json);
		} catch (Exception e) {
			log.error("Error serializing search response", e);
			res.status(Status.INTERNAL_SERVER_ERROR_500).send();
		}
	}
}
