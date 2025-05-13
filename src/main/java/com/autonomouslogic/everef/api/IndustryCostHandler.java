package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Objects;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Tag(name = "industry")
@Path("/v1/industry/cost")
public class IndustryCostHandler implements HttpService, Handler {
	@Inject
	protected ObjectMapper objectMapper;

	private final ObjectMapper queryStringMapper;

	@Inject
	protected IndustryCostHandler(ObjectMapper objectMapper) {
		queryStringMapper = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	@GET
	@ApiResponse(
			responseCode = "200",
			description = "Success",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = IndustryCost.class)))
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
			name = "input",
			schema = @Schema(implementation = IndustryCostInput.class),
			explode = Explode.TRUE)
	public IndustryCost industryCost(IndustryCostInput input) {
		var cost = IndustryCost.builder().input(input);
		return cost.build();
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		var input = createInput(req);
		validateInput(input);
		var result = industryCost(input);
		var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(result);
		res.status(Status.OK_200).send(json);
	}

	private IndustryCostInput createInput(ServerRequest req) {
		var query = req.query();
		var input = new HashMap<String, Object>();
		for (var name : query.names()) {
			var values = query.all(name);
			if (values.size() == 1) {
				input.put(name, values.getFirst());
			} else {
				input.put(name, values);
			}
		}
		try {
			return queryStringMapper.convertValue(input, IndustryCostInput.class);
		} catch (IllegalArgumentException e) {
			throw new ClientException(ExceptionUtils.getMessage(e));
		}
	}

	private void validateInput(IndustryCostInput input) {
		if (input.getProductTypeIds().isEmpty()) {
			throw new ClientException("At least one product type id must be provided");
		}
		if (input.getRuns() < 1) {
			throw new ClientException("Runs must be at least 1");
		}
		if ((input.getSystemId() == null) == (input.getSystemCostIndex() == null)) {
			throw new ClientException("Exactly one of system ID and system cost index must be provided");
		}
		if ((input.getSystemId() == null) == (input.getSecurityClass() == null)) {
			throw new ClientException("Exactly one of system ID and system security class must be provided");
		}
		if ((input.getRigTypeIds() != null && !input.getRigTypeIds().isEmpty())
				&& (input.getMeRigTechLevel() != null
						|| input.getTeRigTechLevel() != null
						|| input.getInventionRigTechLevel() != null
						|| input.getCopyingRigTechLevel() != null)) {
			throw new ClientException("Both rig type IDs and rig tech levels cannot be provided at the same time");
		}
	}

	@Override
	public void routing(HttpRules rules) {
		var path = Objects.requireNonNull(IndustryCostHandler.class.getAnnotation(Path.class))
				.value();
		rules.get(path, this);
		rules.any(StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
	}
}
