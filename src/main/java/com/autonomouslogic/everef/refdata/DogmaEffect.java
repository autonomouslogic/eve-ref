package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class DogmaEffect {
	@JsonProperty
	Long effectId;

	@JsonProperty
	String name;

	@JsonProperty
	Map<String, String> displayName;

	@JsonProperty
	Map<String, String> description;

	@JsonProperty
	Boolean disallowAutoRepeat;

	@JsonProperty
	Integer effectCategory;

	@JsonProperty
	String effectName;

	@JsonProperty
	Boolean electronicChance;

	@JsonProperty
	String guid;

	@JsonProperty
	Long dischargeAttributeId;

	@JsonProperty
	Long durationAttributeId;

	@JsonProperty
	Long falloffAttributeId;

	@JsonProperty
	Long rangeAttributeId;

	@JsonProperty
	Long npcUsageChanceAttributeId;

	@JsonProperty
	Long trackingSpeedAttributeId;

	@JsonProperty
	Long npcActivationChanceAttributeId;

	@JsonProperty
	Long fittingUsageChanceAttributeId;

	@JsonProperty
	Long resistanceAttributeId;

	@JsonProperty
	Integer distribution;

	@JsonProperty
	Long iconId;

	@JsonProperty
	Boolean isAssistance;

	@JsonProperty
	Boolean isOffensive;

	@JsonProperty
	Boolean isWarpSafe;

	@JsonProperty
	Boolean propulsionChance;

	@JsonProperty
	Boolean published;

	@JsonProperty
	Boolean rangeChance;

	@JsonProperty
	String sfxName;

	@JsonProperty
	List<ModifierInfo> modifiers;
}
