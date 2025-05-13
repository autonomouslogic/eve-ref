package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
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
import java.util.Objects;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Tag(name = "industry")
@Path("/v1/industry/cost")
public class IndustryCostHandler implements HttpService, Handler {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected IndustryCostHandler() {}

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
	//	@Parameters(
	//			value = {
	//				@Parameter(
	//						in = ParameterIn.QUERY,
	//						name = "product_type_ids",
	//						schema = @Schema(implementation = Long.class),
	//						array = @ArraySchema(minItems = 1, uniqueItems = true)
	//				),
	//				@Parameter(
	//						in = ParameterIn.QUERY,
	//					schema = @Schema(implementation = Integer.class),
	//						name = "runs"),
	//				@Parameter(
	//						in = ParameterIn.QUERY,
	//					schema = @Schema(implementation = Long.class),
	//						name = "decryptor_type_id"),
	//				@Parameter(
	//						in = ParameterIn.QUERY,
	//					schema = @Schema(implementation = Long.class),
	//						name = "system_id"),
	//				@Parameter(
	//						in = ParameterIn.QUERY,
	//					schema = @Schema(implementation = BigDecimal.class),
	//						name = "system_cost_index"),
	//				@Parameter(
	//						in = ParameterIn.QUERY,
	//					schema = @Schema(implementation = Long.class),
	//						name = "structure_type_id"),
	//				@Parameter(
	//						in = ParameterIn.QUERY,
	//					schema = @Schema(implementation = Long.class),
	//						name = "rig_type_ids",
	//					array = @ArraySchema(minItems = 1, uniqueItems = true)
	//				)
	//			})
	public IndustryCost industryCost(IndustryCostInput input) {
		return IndustryCost.builder().build();
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		try {
			var result = industryCost(IndustryCostInput.builder().build());
			res.status(Status.OK_200)
					.send(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(result));
		} catch (Exception e) {
			res.send();
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
