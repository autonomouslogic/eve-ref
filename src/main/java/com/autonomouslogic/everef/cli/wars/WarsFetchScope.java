package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

/**
 * Calculates which wars to fetch based on current ESI data and stored state.
 */
@Value
@Builder
@Log4j2
public class WarsFetchScope {
	Set<Long> warIds;
	long maxWarId;
	int unfinishedCount;
	int unknownCount;

	public static WarsFetchScope calculate(WarsApi warsApi, EsiHelper esiHelper, Map<Long, JsonNode> warsMap) {
		// Fetch all war IDs from ESI
		var allWarIds = fetchAllWarIds(warsApi, esiHelper);
		var maxWarId = allWarIds.stream().mapToLong(Long::longValue).max().orElse(0L);

		// Find unfinished wars in store
		var unfinishedWars = findUnfinishedWars(warsMap);
		var unfinishedCount = unfinishedWars.size();

		// Find unknown wars
		var unknownWars = findUnknownWars(allWarIds, warsMap, maxWarId);
		var unknownCount = unknownWars.size();

		// Combine both sets
		var warIds = new HashSet<>(unfinishedWars);
		warIds.addAll(unknownWars);

		log.info("Wars fetch scope: {} total, {} unfinished, {} unknown", warIds.size(), unfinishedCount, unknownCount);

		return WarsFetchScope.builder()
				.warIds(warIds)
				.maxWarId(maxWarId)
				.unfinishedCount(unfinishedCount)
				.unknownCount(unknownCount)
				.build();
	}

	private static Set<Long> fetchAllWarIds(WarsApi warsApi, EsiHelper esiHelper) {
		log.info("Fetching all war IDs from ESI");
		// Wars endpoint uses cursor-based pagination with maxWarId, not page-based
		// For simplicity, fetch all wars in one call (the list isn't typically large enough to paginate)
		var warIds = esiHelper.fetchPages(page -> warsApi.getWarsWithHttpInfo(null, null, null));
		return warIds.stream().map(Long::valueOf).collect(Collectors.toSet());
	}

	private static Set<Long> findUnfinishedWars(Map<Long, JsonNode> warsMap) {
		var unfinished = new HashSet<Long>();
		for (var entry : warsMap.entrySet()) {
			var war = entry.getValue();
			if (!war.has("finished") || war.get("finished").isNull()) {
				unfinished.add(entry.getKey());
			}
		}
		return unfinished;
	}

	private static Set<Long> findUnknownWars(Set<Long> allWarIds, Map<Long, JsonNode> warsMap, long maxStoredWarId) {
		var unknown = new HashSet<Long>();
		for (var warId : allWarIds) {
			if (!warsMap.containsKey(warId)) {
				unknown.add(warId);
			}
		}
		return unknown;
	}
}
