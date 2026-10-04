package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class NpcCharacter {
	@JsonProperty
	Long characterId;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> name;

	@JsonProperty
	String description;

	@JsonProperty
	Long corporationId;

	@JsonProperty
	Long locationId;

	@JsonProperty
	Long raceId;

	@JsonProperty
	Long bloodlineId;

	@JsonProperty
	Long ancestryId;

	@JsonProperty
	Long careerId;

	@JsonProperty
	Long schoolId;

	@JsonProperty
	Long specialityId;

	@JsonProperty
	String startDate;

	@JsonProperty
	List<NpcCharacterSkill> skills;

	@JsonProperty
	Boolean ceo;

	@JsonProperty
	Boolean gender;

	@JsonProperty
	Boolean uniqueName;

	@JsonProperty
	@Schema(description = "Only present if this character is also an agent.")
	NpcCharacterAgentInfo agent;
}
