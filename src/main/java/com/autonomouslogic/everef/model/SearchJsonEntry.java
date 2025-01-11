package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class SearchJsonEntry {
	@JsonProperty
	public String text;

	@JsonProperty
	public String query;

	@JsonProperty
	public long id;

	@JsonProperty
	public String link;

	@JsonProperty
	public String type;
}
