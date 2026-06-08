package com.autonomouslogic.everef.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class IndexFileEntry {
	@JsonProperty
	String name;

	@JsonProperty
	long size;

	@JsonProperty
	Instant lastModified;

	@JsonProperty
	String md5;

	@JsonProperty
	String type;

	@JsonProperty
	Instant date;
}
