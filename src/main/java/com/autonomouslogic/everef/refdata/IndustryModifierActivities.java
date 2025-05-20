package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndustryModifierActivities {
	IndustryModifierBonuses researchMaterial;
	IndustryModifierBonuses researchTime;
	IndustryModifierBonuses manufacturing;
	IndustryModifierBonuses invention;
	IndustryModifierBonuses copying;
}
