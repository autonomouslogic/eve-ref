package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.IndustryCost;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@OpenAPIDefinition()
@Tag(name = "industry")
@Path("/v1/industry")
public class IndustryApi {
	@GET
	@Path("/cost")
	@ApiResponse(
			responseCode = "200",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = IndustryCost.class)))
	@Parameters(
			value = {
				@Parameter(
						in = ParameterIn.QUERY,
						name = "product_type_ids",
						schema = @Schema(implementation = Long.class),
						array = @ArraySchema(minItems = 1, uniqueItems = true)),
				@Parameter(
						in = ParameterIn.QUERY,
						name = "product_type_ids",
						array = @ArraySchema(minItems = 1, uniqueItems = true))
			})
	public void industryCost() {}
}
