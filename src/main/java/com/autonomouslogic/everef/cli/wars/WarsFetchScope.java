package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Calculates which wars to fetch based on current ESI data and stored state.
 */
@Log4j2
public class WarsFetchScope {
	private final Set<Long> warIds;
	private final long maxWarId;
	private final int unfinishedCount;
	private final int unknownCount;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected ObjectMapper objectMapper;

	private WarsFetchScope(Set<Long> warIds, long maxWarId, int unfinishedCount, int unknownCount) {
		this.warIds = warIds;
		this.maxWarId = maxWarId;
		this.unfinishedCount = unfinishedCount;
		this.unknownCount = unknownCount;
	}

	@Inject
	protected WarsFetchScope() {
		this(Set.of(), 0, 0, 0);
	}

	public Set<Long> getWarIds() {
		return warIds;
	}

	public long getMaxWarId() {
		return maxWarId;
	}

	public int getUnfinishedCount() {
		return unfinishedCount;
	}

	public int getUnknownCount() {
		return unknownCount;
	}

	public WarsFetchScope calculate(Map<Long, JsonNode> warsMap) {
		// Fetch all war IDs from ESI (only unfinished wars)
		var allWarIds = fetchAllWarIds();
		var maxWarId = allWarIds.stream().mapToLong(Long::longValue).max().orElse(0L);

		// Find unfinished wars in store
		var unfinishedWars = findUnfinishedWars(warsMap);
		var unfinishedCount = unfinishedWars.size();

		// Find unknown wars from API
		var unknownWars = findUnknownWars(allWarIds, warsMap);
		var unknownCount = unknownWars.size();

		// Find wars in current state that might have changed (both finished and unfinished)
		var existingWars = new HashSet<>(warsMap.keySet());

		// Combine: existing wars + unknown wars from API
		var warIds = new HashSet<>(existingWars);
		warIds.addAll(allWarIds);

		log.info("Wars fetch scope: {} total, {} unfinished, {} unknown", warIds.size(), unfinishedCount, unknownCount);

		return new WarsFetchScope(warIds, maxWarId, unfinishedCount, unknownCount);
	}

	private Set<Long> fetchAllWarIds() {
		log.info("Fetching all war IDs from ESI");
		var url = Configs.ESI_BASE_URL.getRequired() + "wars/";
		var warIds = new HashSet<Long>();
		var page = 1;
		var hasMore = true;

		while (hasMore) {
			try (var response = okHttpWrapper.get(url + "?page=" + page)) {
				if (response.code() != 200) {
					throw new RuntimeException("Failed to fetch wars page " + page + ": HTTP " + response.code());
				}
				var body = response.body();
				if (body == null) {
					throw new RuntimeException("Empty response body for wars page " + page);
				}

				var json = objectMapper.readValue(body.string(), int[].class);
				for (var warId : json) {
					warIds.add((long) warId);
				}

				var pagesHeader = response.header("X-Pages");
				var totalPages = pagesHeader != null ? Integer.parseInt(pagesHeader) : 1;
				hasMore = page < totalPages;
				page++;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return warIds;
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

	private static Set<Long> findUnknownWars(Set<Long> allWarIds, Map<Long, JsonNode> warsMap) {
		var unknown = new HashSet<Long>();
		for (var warId : allWarIds) {
			if (!warsMap.containsKey(warId)) {
				unknown.add(warId);
			}
		}
		return unknown;
	}
}
