package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
		description = "Details about which types a mutaplasmid can be applied to to create another type. "
				+ "These are created by EVE Ref and derived from Hoboleaks.")
public class MutaplasmidTypeMapping {
	@JsonProperty
	long resultingTypeId;

	@JsonProperty
	List<Long> applicableTypeIds;
}
