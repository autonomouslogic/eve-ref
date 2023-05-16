package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
class RegionTypePair {
	int regionId;
	int typeId;

	public static RegionTypePair fromHistory(MarketHistoryEntry entry) {
		return new RegionTypePair(entry.getRegionId(), entry.getTypeId());
	}

	public static RegionTypePair fromHistory(JsonNode entry) {
		return new RegionTypePair(
				entry.get("region_id").asInt(), entry.get("type_id").asInt());
	}
}
