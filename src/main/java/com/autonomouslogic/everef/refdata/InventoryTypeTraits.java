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
	@Schema(
			description = "Misc bonuses indexed by 'importance' from the original array. "
					+ "This is represented as an array in the SDE, but EVE Ref converts it to a map.")
	@JsonProperty
	Map<String, TraitBonus> miscBonuses;

	@Schema(
			description = "Role bonuses indexed by 'importance' from the original array. "
					+ "This is represented as an array in the SDE, but EVE Ref converts it to a map.")
	@JsonProperty
	Map<String, TraitBonus> roleBonuses;

	@Schema(
			description = "Type traits. " + "First key is type ID granting the bonus. "
					+ "Second key is the `important` from the original array. "
					+ "This is represented as an array in the SDE, but EVE Ref converts it to a map.")
	@JsonProperty
	Map<String, Map<String, TraitBonus>> types;

	@JsonProperty
	Long iconId;
}
