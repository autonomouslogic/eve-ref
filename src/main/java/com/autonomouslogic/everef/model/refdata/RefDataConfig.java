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
	String outputStore;

	@JsonProperty
	@lombok.Builder.Default
	boolean dedicatedOutput = true;

	@JsonProperty
	String outputFile;

	@JsonProperty
	String model;

	@JsonProperty
	RefTypeConfig sde;

	@JsonProperty
	RefTypeConfig esi;

	@JsonProperty
	RefTypeConfig hoboleaks;

	@JsonProperty
	RefTestConfig test;
}
