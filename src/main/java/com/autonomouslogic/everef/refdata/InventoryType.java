package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "An inventory type")
public class InventoryType {
	@JsonProperty
	Long typeId;

	@JsonProperty
	BigDecimal basePrice;

	@JsonProperty
	Double capacity;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> description;

	@JsonProperty
	@Schema(description = "A map of dogma attributes. The key is the attribute ID")
	Map<String, DogmaTypeAttribute> dogmaAttributes;

	@JsonProperty
	@Schema(description = "A map of dogma attributes. The key is the attribute ID")
	Map<String, DogmaTypeEffect> dogmaEffects;

	@JsonProperty
	Long factionId;

	@JsonProperty
	Long graphicId;

	@JsonProperty
	Long groupId;

	@JsonProperty
	Long iconId;

	@JsonProperty
	Long marketGroupId;

	@JsonProperty
	BigDecimal mass;

	@JsonProperty
	Map<String, List<Integer>> masteries;

	@JsonProperty
	Long metaGroupId;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> name;

	@JsonProperty
	Double packagedVolume;

	@JsonProperty
	Integer portionSize;

	@JsonProperty
	Boolean published;

	@JsonProperty
	Long raceId;

	@JsonProperty
	Double radius;

	@JsonProperty
	String sofFactionName;

	@JsonProperty
	Long sofMaterialSetId;

	@JsonProperty
	Long soundId;

	@JsonProperty
	InventoryTypeTraits traits;

	@JsonProperty
	Long variationParentTypeId;

	@JsonProperty
	BigDecimal volume;

	@JsonProperty("is_skill")
	@Schema(defaultValue = "false", description = "Whether this type is a skill or not. This is added by EVE Ref.")
	Boolean skill;

	@JsonProperty
	@Schema(
			description = "The skills required for this type. The key is the skill type ID and the value is the level. "
					+ "This is added by EVE Ref and derived from dogma attributes.")
	Map<Long, Integer> requiredSkills;

	@JsonProperty("is_mutaplasmid")
	@Schema(
			defaultValue = "false",
			description = "Whether this type is a mutaplasmid or not. This is added by EVE Ref.")
	Boolean mutaplasmid;

	@JsonProperty
	@Schema(
			description =
					"Which mutaplasmids can be applied to this type to create a dynamic item. This is added by EVE Ref.")
	List<Long> applicableMutaplasmidTypeIds;

	@JsonProperty
	@Schema(description = "Which mutaplasmids can used to create this dynamic item. This is added by EVE Ref.")
	List<Long> creatingMutaplasmidTypeIds;

	@JsonProperty("is_dynamic_item")
	@Schema(
			defaultValue = "false",
			description =
					"Whether this type is a dynamic item created by a mutaplasmid or not. This is added by EVE Ref.")
	Boolean dynamicItem;

	@JsonProperty
	@Schema(
			description =
					"The variations for this type. The key is the meta group and the value is a list of type IDs. "
							+ "This is added by EVE Ref.")
	Map<Long, List<Long>> typeVariations;

	@JsonProperty
	@Schema(
			description =
					"The variations for this ore type. The key is the asteroid meta level and the value is a list of type IDs. "
							+ "This is added by EVE Ref.")
	Map<Long, List<Long>> oreVariations;

	@JsonProperty
	@Schema(description = "Whether this is an ore or not. This is added by EVE Ref.")
	Boolean isOre;

	@JsonProperty("is_blueprint")
	@Schema(defaultValue = "false", description = "Whether this type is a blueprint or not. This is added by EVE Ref.")
	Boolean blueprint;

	@JsonProperty
	@Schema(
			description = "The blueprints producing this type. The key is the blueprint type ID. "
					+ "This is added by EVE Ref.")
	Map<Long, ProducingBlueprint> producedByBlueprints;

	@JsonProperty
	Map<Long, TypeMaterial> typeMaterials;

	@JsonProperty
	@Schema(description = "Types this can be fitted to. This is added by EVE Ref.")
	List<Long> canFitTypes;

	@JsonProperty
	@Schema(description = "Types which can be fitted. This is added by EVE Ref.")
	List<Long> canBeFittedWithTypes;

	@JsonProperty
	@Schema(description = "The schematics consuming this type. This is added by EVE Ref.")
	List<Long> usedBySchematicIds;

	@JsonProperty
	@Schema(description = "The schematics producing this type. This is added by EVE Ref.")
	List<Long> producedBySchematicIds;

	@JsonProperty
	@Schema(
			description =
					"The type IDs for the planetary extractor pins which can be used to harvest this type. This is added by EVE Ref.")
	List<Long> harvestedByPinTypeIds;

	@JsonProperty
	@Schema(
			description =
					"The type IDs for the planetary pins which can be built on this planet. This is added by EVE Ref.")
	List<Long> buildablePinTypeIds;

	@JsonProperty
	@Schema(
			description =
					"The schematic IDs which can be installed into this planetary processor. This is added by EVE Ref.")
	List<Long> installableSchematicIds;

	@JsonProperty
	@Schema(
			description =
					"The blueprints in which this type is used. The first key is the blueprint ID and the second key is the activity name. "
							+ "This is added by EVE Ref.")
	Map<Long, Map<String, UsedInBlueprint>> usedInBlueprints;

	@JsonProperty
	@Schema(description = "For structure engineering rigs, these are the category IDs the rig affects in some way. " +
		"This is added by EVE Ref.")
	List<Long> engineeringRigAffectedCategoryIds;

	@JsonProperty
	@Schema(description = "For structure engineering rigs, these are the group IDs the rig affects in some way. " +
		"This is added by EVE Ref.")
	List<Long> engineeringRigAffectedGroupIds;
}
