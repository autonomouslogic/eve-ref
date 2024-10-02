package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class Bundle {
	@Schema(description = "A map of types. The key is the type ID.")
	Map<String, InventoryType> types;

	@Schema(description = "A map of dogma attributes. The key is the attribute ID.")
	Map<String, DogmaAttribute> dogmaAttributes;

	@Schema(description = "A map of skills. The key is the skill ID.")
	Map<String, Skill> skills;

	@Schema(description = "A map of units. The key is the unit ID.")
	Map<String, Unit> units;

	@Schema(description = "A map of icons. The key is the icon ID.")
	Map<String, Icon> icons;

	@Schema(description = "A map of market groups. The key is the market group ID.")
	Map<String, MarketGroup> marketGroups;

	@Schema(description = "A map of inventory categories. The key is the category ID.")
	Map<String, InventoryCategory> categories;

	@Schema(description = "A map of inventory groups. The key is the group ID.")
	Map<String, InventoryGroup> groups;

	@Schema(description = "A map of meta groups. The key is the group ID.")
	Map<String, MetaGroup> metaGroups;

	@Schema(description = "A map of blueprints. The key is the blueprint type ID.")
	Map<String, Blueprint> blueprints;
}
