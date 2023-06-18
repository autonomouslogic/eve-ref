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
	long typeId;

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
	Integer factionId;

	@JsonProperty
	Integer graphicId;

	@JsonProperty
	Integer groupId;

	@JsonProperty
	Integer iconId;

	@JsonProperty
	Integer marketGroupId;

	@JsonProperty
	BigDecimal mass;

	@JsonProperty
	Map<String, List<Integer>> masteries;

	@JsonProperty
	Integer metaGroupId;

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
	Integer raceId;

	@JsonProperty
	Double radius;

	@JsonProperty
	String sofFactionName;

	@JsonProperty
	Integer sofMaterialSetId;

	@JsonProperty
	Integer soundId;

	@JsonProperty
	InventoryTypeTraits traits;

	@JsonProperty
	Long variationParentTypeId;

	@JsonProperty
	BigDecimal volume;

	@JsonProperty("is_skill")
	@Schema(defaultValue = "false", description = "Whether this type is a skill or not. This is added by EVE Ref.")
	boolean skill;

	@JsonProperty
	@Schema(
			description = "The skills required for this type. The key is the skill type ID and the value is the level. "
					+ "This is added by EVE Ref and derived from dogma attributes.")
	Map<Long, Integer> requiredSkills;

	@JsonProperty("is_mutaplasmid")
	@Schema(
			defaultValue = "false",
			description = "Whether this type is a mutaplasmid or not. This is added by EVE Ref.")
	boolean mutaplasmid;

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
	boolean dynamicItem;

	@JsonProperty
	@Schema(
			description =
					"The variations for this type. The key is the meta group and the value is a list of type IDs. "
							+ "This is added by EVE Ref.")
	Map<Long, List<Long>> typeVariations;

	@JsonProperty("is_blueprint")
	@Schema(defaultValue = "false", description = "Whether this type is a blueprint or not. This is added by EVE Ref.")
	boolean blueprint;

	@JsonProperty
	@Schema(
			description = "The blueprints creating this type. The key is the blueprint type ID. "
					+ "This is added by EVE Ref.")
	Map<Long, CreatingBlueprint> creatingBlueprints;

	@JsonProperty
	Map<Long, TypeMaterial> typeMaterials;
}
