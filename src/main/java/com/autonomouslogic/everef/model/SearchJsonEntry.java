package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public final class SearchJsonEntry {
	@JsonProperty
	public String text;

	@JsonProperty
	public long id;

	@JsonProperty
	public String link;

	@JsonProperty
	public String type;
}
