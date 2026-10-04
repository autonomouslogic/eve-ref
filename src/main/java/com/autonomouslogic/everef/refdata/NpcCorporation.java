package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class NpcCorporation {
	@JsonProperty
	Long corporationId;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> name;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> description;

	@JsonProperty
	String tickerName;

	@JsonProperty
	Long raceId;

	@JsonProperty
	Long factionId;

	@JsonProperty
	Long ceoId;

	@JsonProperty
	Long stationId;

	@JsonProperty
	Long solarSystemId;

	@JsonProperty
	Long iconId;

	@JsonProperty
	String size;

	@JsonProperty
	String extent;

	@JsonProperty
	BigDecimal sizeFactor;

	@JsonProperty
	Long memberLimit;

	@JsonProperty
	Long minimumJoinStanding;

	@JsonProperty
	BigDecimal minSecurity;

	@JsonProperty
	BigDecimal taxRate;

	@JsonProperty
	Long shares;

	@JsonProperty
	BigDecimal initialPrice;

	@JsonProperty
	Boolean uniqueName;

	@JsonProperty
	Boolean deleted;

	@JsonProperty
	List<Long> allowedMemberRaces;

	@JsonProperty
	@Schema(description = "The key is the division ID, referencing npc_corporation_divisions.")
	Map<Long, NpcCorporationDivisionAssignment> divisions;

	@JsonProperty
	Long mainActivityId;

	@JsonProperty
	Long secondaryActivityId;

	@JsonProperty
	Long friendId;

	@JsonProperty
	Long enemyId;

	@JsonProperty
	Boolean sendCharTerminationMessage;

	@JsonProperty
	Boolean hasPlayerPersonnelManager;
}
