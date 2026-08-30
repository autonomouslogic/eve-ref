package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"type_id",
	"name",
	"manufacturing_time_bonus",
	"blueprintmanufacture_time_bonus",
	"copy_speed_bonus",
	"advanced_industry_skill_industry_job_time_bonus",
	"mineral_need_research_bonus",
	"manufacture_time_per_level",
	"reaction_time_bonus",
	"datacore",
	"encryption_methods"
})
@Schema
public class IndustrySkill {
	@JsonProperty
	long typeId;

	@JsonProperty
	String name;

	@JsonProperty
	Double manufacturingTimeBonus;

	@JsonProperty
	Double blueprintmanufactureTimeBonus;

	@JsonProperty
	Double copySpeedBonus;

	@JsonProperty
	Double advancedIndustrySkillIndustryJobTimeBonus;

	@JsonProperty
	Double mineralNeedResearchBonus;

	@JsonProperty
	Double manufactureTimePerLevel;

	@JsonProperty
	Double reactionTimeBonus;

	@JsonProperty
	boolean datacore;

	@JsonProperty
	boolean encryptionMethods;
}
