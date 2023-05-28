package com.autonomouslogic.everef.model.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class RefDataConfig {
	@JsonProperty
	String id;

	@JsonProperty
	String idField;

	@JsonProperty
	String outputFile;

	@JsonProperty
	String model;

	@JsonProperty
	RefTypeConfig sde;

	@JsonProperty
	RefTypeConfig esi;

	@JsonProperty
	RefTestConfig test;
}
