package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class InventoryTypeTraits {
	@JsonProperty
	Map<String, TraitBonus> miscBonuses;

	@JsonProperty
	Map<String, TraitBonus> roleBonuses;

	@JsonProperty
	Map<String, Map<String, TraitBonus>> types;

	@JsonProperty
	Integer iconId;
}
