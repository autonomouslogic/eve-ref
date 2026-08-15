package com.autonomouslogic.everef.model.api.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
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
@Schema
public class SearchEntry {
	@JsonProperty
	String title;

	@JsonProperty
	String language;

	@JsonProperty
	long id;

	@JsonProperty
	String typeName;

	@JsonProperty
	SearchEntryType type;

	@JsonProperty
	SearchEntryUrls urls;

	@JsonIgnore
	@Hidden
	String query;

	@Schema(description = "Relevance score of the search result. Lower is better.")
	@JsonProperty
	long relevance;
}
