package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Tag(name = "industry")
@Path("/v1/industry")
public class IndustryApi {
	@GET
	@Path("/cost")
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
	public void industryCost() {}
}
