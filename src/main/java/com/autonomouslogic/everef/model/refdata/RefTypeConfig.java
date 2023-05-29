package com.autonomouslogic.everef.model.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class RefTypeConfig {
	@JsonProperty
	String file;

	@JsonProperty
	Map<String, String> renames;

	@JsonProperty
	List<String> languageAttributes;

	@JsonProperty
	Map<String, String> arrayToObjects;

	@JsonProperty
	List<String> removes;
}
