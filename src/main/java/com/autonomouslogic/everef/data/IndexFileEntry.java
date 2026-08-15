package com.autonomouslogic.everef.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class IndexFileEntry {
	@JsonProperty
	String name;

	@JsonProperty
	String url;

	@JsonProperty
	long size;

	@JsonProperty
	Instant lastModified;

	@JsonProperty
	String etag;

	@JsonProperty
	String type;

	@JsonProperty
	Instant fileTime;

	@JsonProperty
	Long sequenceNumber;
}
