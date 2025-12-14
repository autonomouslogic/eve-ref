package com.autonomouslogic.everef.model.api.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema
@AllArgsConstructor
@Getter
public enum SearchEntryType {
	@JsonProperty("inventory_type")
	INVENTORY_TYPE("types", "types");

	private final String eveRefType;
	private final String refDataType;
}
