package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"type_id",
	"name",
	"time_bonus",
	"material_bonus",
	"thukker_material_bonus",
	"cost_bonus",
	"manufacturing_categories",
	"manufacturing_groups",
	"reaction_categories",
	"reaction_groups",
	"global_activities",
	"high_sec_mod",
	"low_sec_mod",
	"null_sec_mod"
})
@Schema
public class IndustryRig {
	@JsonProperty
	long typeId;

	@JsonProperty
	String name;

	@JsonProperty
	double timeBonus;

	@JsonProperty
	double materialBonus;

	@JsonProperty
	double thukkerMaterialBonus; // @todo unsupported https://github.com/autonomouslogic/eve-ref/issues/839

	@JsonProperty
	double costBonus;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<Long> manufacturingCategories;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<Long> manufacturingGroups;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<Long> reactionCategories;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<Long> reactionGroups;

	@JsonProperty
	List<String> globalActivities;

	@JsonProperty
	double highSecMod;

	@JsonProperty
	double lowSecMod;

	@JsonProperty
	double nullSecMod;
}
