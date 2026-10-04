package com.autonomouslogic.everef.refdata;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
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
						version = "dev",
						contact = @Contact(name = "Kenn", url = "https://everef.net/discord")),
		servers = @Server(url = ReferenceDataSpec.BASE_URL),
		externalDocs =
				@ExternalDocumentation(
						description = "Reference data",
						url = "https://docs.everef.net/datasets/reference-data.html"))
@Tag(name = "refdata")
public interface ReferenceDataSpec {
	String BASE_URL = "https://ref-data.everef.net";

	@GET
	@Path("/meta")
	@Operation(description = "Get metadata about the API.")
	@ApiResponse(
			responseCode = "200",
			description = "The metadata.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	RefDataMeta getMeta();

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
	InventoryCategory getCategory(@PathParam("category_id") long categoryId);

	@GET
	@Path("/categories/bundle")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The categories bundle.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Bundle getCategoriesBundle();

	@GET
	@Path("/categories/{category_id}/bundle")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The category bundle.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Bundle getCategoryBundle(@PathParam("category_id") long categoryId);

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
	InventoryGroup getGroup(@PathParam("group_id") long groupId);

	@GET
	@Path("/groups/{group_id}/bundle")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The group bundle.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Bundle getGroupBundle(@PathParam("group_id") long groupId);

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
	@Path("/market_groups/root")
	@Operation(description = "Get all root market group IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Root market group IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getRootMarketGroups();

	@GET
	@Path("/market_groups/root/bundle")
	@Operation(description = "Get bundle for root market groups.")
	@ApiResponse(
			responseCode = "200",
			description = "Root market group bundle.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Bundle getRootMarketGroupsBundle();

	@GET
	@Path("/market_groups/{market_group_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The market group.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	MarketGroup getMarketGroup(@PathParam("market_group_id") long marketGroupId);

	@GET
	@Path("/market_groups/{market_group_id}/bundle")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The market group bundle.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Bundle getMarketGroupBundle(@PathParam("market_group_id") long marketGroupId);

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
	MetaGroup getMetaGroup(@PathParam("meta_group_id") long metaGroupId);

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
	InventoryType getType(@PathParam("type_id") long typeId);

	@GET
	@Path("/types/{type_id}/bundle")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The type bundle.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Bundle getTypeBundle(@PathParam("type_id") long typeId);

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
	DogmaAttribute getDogmaAttribute(@PathParam("attribute_id") long attributeId);

	@GET
	@Path("/dogma_effects")
	@Operation(description = "Get all dogma effect IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Dogma effect IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllDogmaEffects();

	@GET
	@Path("/dogma_effects/{effect_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The dogma effect.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	DogmaEffect getDogmaEffect(@PathParam("effect_id") long effectId);

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
	Skill getSkill(@PathParam("skill_type_id") long skillTypeId);

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
	Mutaplasmid getMutaplasmid(@PathParam("mutaplasmid_type_id") long mutaplasmidTypeId);

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
	@Path("/units/{unit_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The unit.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Unit getUnit(@PathParam("unit_id") long unitId);

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
	Blueprint getBlueprint(@PathParam("blueprint_type_id") long blueprintTypeId);

	@GET
	@Path("/icons")
	@Operation(description = "Get all icon IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Icon IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllIcons();

	@GET
	@Path("/icons/{icon_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The icon.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Icon getIcon(@PathParam("icon_id") long iconId);

	@GET
	@Path("/schematics")
	@Operation(description = "Get all schematic IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Schematic type IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Long> getAllSchematics();

	@GET
	@Path("/schematics/{schematic_id}")
	@Operation
	@ApiResponse(
			responseCode = "200",
			description = "The schematic.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Schematic getSchematic(@PathParam("schematic_id") long schematicId);

	@GET
	@Path("/regions")
	@Operation(description = "Get all region IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Region IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllRegions();

	@GET
	@Path("/regions/{region_id}")
	@Operation(description = "Get a region.")
	@ApiResponse(
			responseCode = "200",
			description = "The region.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Region getRegion(@PathParam("region_id") long regionId);

	@GET
	@Path("/agent_types")
	@Operation(description = "Get all agent type IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Agent type IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllAgentTypes();

	@GET
	@Path("/agent_types/{agent_type_id}")
	@Operation(description = "Get an agent type.")
	@ApiResponse(
			responseCode = "200",
			description = "The agent type.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	AgentType getAgentType(@PathParam("agent_type_id") long agentTypeId);

	@GET
	@Path("/npc_corporation_divisions")
	@Operation(description = "Get all NPC corporation division IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "NPC corporation division IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllNpcCorporationDivisions();

	@GET
	@Path("/npc_corporation_divisions/{division_id}")
	@Operation(description = "Get an NPC corporation division.")
	@ApiResponse(
			responseCode = "200",
			description = "The NPC corporation division.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	NpcCorporationDivision getNpcCorporationDivision(@PathParam("division_id") long divisionId);

	@GET
	@Path("/npc_corporations")
	@Operation(description = "Get all NPC corporation IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "NPC corporation IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllNpcCorporations();

	@GET
	@Path("/npc_corporations/{corporation_id}")
	@Operation(description = "Get an NPC corporation.")
	@ApiResponse(
			responseCode = "200",
			description = "The NPC corporation.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	NpcCorporation getNpcCorporation(@PathParam("corporation_id") long corporationId);

	@GET
	@Path("/npc_characters")
	@Operation(description = "Get all NPC character IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "NPC character IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllNpcCharacters();

	@GET
	@Path("/npc_characters/{character_id}")
	@Operation(description = "Get an NPC character.")
	@ApiResponse(
			responseCode = "200",
			description = "The NPC character.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	NpcCharacter getNpcCharacter(@PathParam("character_id") long characterId);

	@GET
	@Path("/constellations")
	@Operation(description = "Get all constellation IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Constellation IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllConstellations();

	@GET
	@Path("/constellations/{constellation_id}")
	@Operation(description = "Get a constellation.")
	@ApiResponse(
			responseCode = "200",
			description = "The constellation.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Constellation getConstellation(@PathParam("constellation_id") long constellationId);

	@GET
	@Path("/solar_systems")
	@Operation(description = "Get all solar system IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Solar system IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllSolarSystems();

	@GET
	@Path("/solar_systems/{solar_system_id}")
	@Operation(description = "Get a solar system.")
	@ApiResponse(
			responseCode = "200",
			description = "The solar system.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	SolarSystem getSolarSystem(@PathParam("solar_system_id") long solarSystemId);

	@GET
	@Path("/planets")
	@Operation(description = "Get all planet IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Planet IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllPlanets();

	@GET
	@Path("/planets/{planet_id}")
	@Operation(description = "Get a planet.")
	@ApiResponse(
			responseCode = "200",
			description = "The planet.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Planet getPlanet(@PathParam("planet_id") long planetId);

	@GET
	@Path("/asteroid_belts")
	@Operation(description = "Get all asteroid belt IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Asteroid belt IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllAsteroidBelts();

	@GET
	@Path("/asteroid_belts/{asteroid_belt_id}")
	@Operation(description = "Get an asteroid belt.")
	@ApiResponse(
			responseCode = "200",
			description = "The asteroid belt.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	AsteroidBelt getAsteroidBelt(@PathParam("asteroid_belt_id") long asteroidBeltId);

	@GET
	@Path("/moons")
	@Operation(description = "Get all moon IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Moon IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllMoons();

	@GET
	@Path("/moons/{moon_id}")
	@Operation(description = "Get a moon.")
	@ApiResponse(
			responseCode = "200",
			description = "The moon.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Moon getMoon(@PathParam("moon_id") long moonId);

	@GET
	@Path("/stars")
	@Operation(description = "Get all star IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Star IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllStars();

	@GET
	@Path("/stars/{star_id}")
	@Operation(description = "Get a star.")
	@ApiResponse(
			responseCode = "200",
			description = "The star.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Star getStar(@PathParam("star_id") long starId);

	@GET
	@Path("/stargates")
	@Operation(description = "Get all stargate IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Stargate IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllStargates();

	@GET
	@Path("/stargates/{stargate_id}")
	@Operation(description = "Get a stargate.")
	@ApiResponse(
			responseCode = "200",
			description = "The stargate.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	Stargate getStargate(@PathParam("stargate_id") long stargateId);

	@GET
	@Path("/npc_stations")
	@Operation(description = "Get all NPC station IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "NPC station IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllNpcStations();

	@GET
	@Path("/npc_stations/{station_id}")
	@Operation(description = "Get an NPC station.")
	@ApiResponse(
			responseCode = "200",
			description = "The NPC station.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	NpcStation getNpcStation(@PathParam("station_id") long stationId);

	@GET
	@Path("/station_operations")
	@Operation(description = "Get all station operation IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Station operation IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllStationOperations();

	@GET
	@Path("/station_operations/{operation_id}")
	@Operation(description = "Get a station operation.")
	@ApiResponse(
			responseCode = "200",
			description = "The station operation.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	StationOperation getStationOperation(@PathParam("operation_id") long operationId);

	@GET
	@Path("/station_services")
	@Operation(description = "Get all station service IDs.")
	@ApiResponse(
			responseCode = "200",
			description = "Station service IDs.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	List<Integer> getAllStationServices();

	@GET
	@Path("/station_services/{service_id}")
	@Operation(description = "Get a station service.")
	@ApiResponse(
			responseCode = "200",
			description = "The station service.",
			useReturnTypeSchema = true,
			content = @Content(mediaType = "application/json"))
	StationService getStationService(@PathParam("service_id") long serviceId);
}
