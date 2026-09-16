package com.autonomouslogic.everef.model.api.search;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "Links to the entity on EVE Ref and in the reference data API.")
public class SearchEntryUrls {
	@JsonProperty
	@Schema(description = "URL to the entity page on everef.net.")
	String everef;

	@JsonProperty
	@Schema(description = "URL to the entity in the EVE Ref reference data API (ref-data.everef.net).")
	String referenceData;
}
