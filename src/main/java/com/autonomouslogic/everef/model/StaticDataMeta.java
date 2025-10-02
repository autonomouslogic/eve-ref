package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Jacksonized
public class StaticDataMeta {
	@JsonProperty("_key")
	public String key;

	@JsonProperty
	public int buildNumber;

	@JsonProperty
	public Instant releaseDate;
}
