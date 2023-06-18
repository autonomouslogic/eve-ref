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
	@Path("/market_groups")
	@Operation(description = "Get all market group IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Market group IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllMarketGroups();

	@GET
	@Path("/market_groups/{market_group_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The market group.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	MarketGroup getMarketGroup(@PathParam("market_group_id") int marketGroupId);

	@GET
	@Path("/meta_groups")
	@Operation(description = "Get all meta group IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Meta group IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllMetaGroups();

	@GET
	@Path("/meta_groups/{meta_group_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The meta group.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	MetaGroup getMetaGroup(@PathParam("meta_group_id") int metaGroupId);

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

	@GET
	@Path("/skills")
	@Operation(description = "Get all skill type IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Skill type IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllSkills();

	@GET
	@Path("/skills/{skill_type_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The skill.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Skill getSkill(@PathParam("skill_type_id") int skillTypeId);

	@GET
	@Path("/mutaplasmids")
	@Operation(description = "Get all mutaplasmid IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Mutaplasmid type IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllMutaplasmids();

	@GET
	@Path("/mutaplasmids/{mutaplasmid_type_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The mutaplasmid.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Mutaplasmid getMutaplasmid(@PathParam("mutaplasmid_type_id") int mutaplasmidTypeId);

	@GET
	@Path("/units")
	@Operation(description = "Get all unit IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Unit type IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllUnits();

	@GET
	@Path("/units/{units_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The unit.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Unit getUnit(@PathParam("units_id") int unitId);

	@GET
	@Path("/blueprints")
	@Operation(description = "Get all blueprint IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Blueprint type IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Long> getAllBlueprints();

	@GET
	@Path("/blueprints/{blueprint_type_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The blueprint.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Blueprint getBlueprint(@PathParam("blueprint_type_id") int blueprintTypeId);
}
