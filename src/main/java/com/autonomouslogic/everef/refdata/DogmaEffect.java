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
	Integer effectId;

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
	Integer dischargeAttributeId;

	@JsonProperty
	Integer durationAttributeId;

	@JsonProperty
	Integer falloffAttributeId;

	@JsonProperty
	Integer rangeAttributeId;

	@JsonProperty
	Integer trackingSpeedAttributeId;

	@JsonProperty
	Integer distribution;

	@JsonProperty
	Integer iconId;

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
	List<ModifierInfo> modifiers;
}
