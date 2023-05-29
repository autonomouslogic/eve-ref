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
	@Path("/categories")
	@Operation(description = "Get all category IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Category IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllCategories();

	@GET
	@Path("/categories/{category_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The category.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	InventoryCategory getCategory(@PathParam("category_id") int categoryId);

	@GET
	@Path("/groups")
	@Operation(description = "Get all type IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Group IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllGroups();

	@GET
	@Path("/groups/{group_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The group.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	InventoryGroup getGroup(@PathParam("group_id") int groupId);

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
			description = "The type.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	InventoryType getType(@PathParam("type_id") int typeId);

	@GET
	@Path("/dogma_attributes")
	@Operation(description = "Get all dogma attribute IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Dogma attribute IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllDogmaAttributes();

	@GET
	@Path("/dogma_attributes/{attribute_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The dogma attribute.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	DogmaAttribute getDogmaAttribute(@PathParam("attribute_id") int attributeId);
}
