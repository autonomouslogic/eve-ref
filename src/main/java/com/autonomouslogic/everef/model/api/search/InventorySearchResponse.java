package com.autonomouslogic.everef.model.api.search;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true,  access = AccessLevel.PUBLIC)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class InventorySearchResponse {

    @Schema(description = "The original search input provided by the user")
    @JsonProperty
    String input;

    @Schema(description = "List of matching inventoryType")
    @JsonProperty("inventory_type")
    @Singular("inventoryType")
    List<SearchInventoryType> searchInventoryTypes;

}
