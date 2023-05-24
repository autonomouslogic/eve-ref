package com.autonomouslogic.everef.refdata;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@OpenAPIDefinition(
		info =
				@Info(
						title = "EVE Ref Reference Data for EVE Online",
						description = "This spec should be considered unstable and subject to change at any time.",
						license =
								@License(
										name = "CCP",
										url = "https://github.com/autonomouslogic/eve-ref/blob/main/LICENSE-CCP"),
						version = "dev"),
		servers = @Server(url = "https://ref-data.everef.net"),
		externalDocs =
				@ExternalDocumentation(
						description = "Reference data",
						url = "https://github.com/autonomouslogic/eve-ref/blob/main/docs/refdata.md"))
@Tag(name = "refdata")
public interface ReferenceDataSpec {
	@GET
	@Path("/types")
	@Operation(description = "Get all type IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Type IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllTypes();

	@GET
	@Path("/types/{type_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "Types.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	InventoryType getType(@PathParam("type_id") int typeId);
}