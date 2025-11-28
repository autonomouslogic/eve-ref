package com.autonomouslogic.everef.model.api.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true, access = AccessLevel.PUBLIC)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class SearchInventoryType {
    @Schema(description = "The unique ID of the inventory type")
    @JsonProperty
    Long typeId;

    @Schema(description = "The English name of the item")
    @JsonProperty
    String nameEn;
}