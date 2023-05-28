package com.autonomouslogic.everef.model.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class RefTestConfig {
	@JsonProperty
	String filename;

	@JsonProperty
	List<Long> ids;
}
