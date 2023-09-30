package com.autonomouslogic.everef.model.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
	Pattern fileRegex;

	@JsonProperty
	Map<String, String> renames;

	@JsonProperty
	List<String> languageAttributes;

	@JsonProperty
	Map<String, String> arrayToObjects;

	@JsonProperty
	List<String> removes;

	/**
	 * If true, the file is a single file containing all the data for this type. If false, the file is an object with many IDs.
	 */
	@JsonProperty
	boolean individualFiles;
}
