package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.log4j.Log4j2;

/**
 * Handles hash corrections for killmails using Zkillboard data.
 * Searches the last 30 days of Zkillboard history to find corrected hashes for failed killmail fetches.
 */
@Log4j2
public class ZkillboardHashCorrector {
	private static final String ZKILLBOARD_BASE_URL = "https://r2z2.zkillboard.com/history/";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final int HISTORY_DAYS = 30;
	private static final String CCP_VERIFIED = "CCP VERIFIED";

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	@Named("esi")
	protected OkHttpWrapper okHttpWrapper;

	private final Map<LocalDate, Map<Long, String>> hashCache = new ConcurrentHashMap<>();

	@Inject
	protected ZkillboardHashCorrector() {}

	/**
	 * Attempts to correct a killmail hash by searching Zkillboard history.
	 *
	 * @param killmailId the killmail ID
	 * @param originalHash the original hash that failed
	 * @return the corrected hash, or empty if not found or CCP VERIFIED
	 */
	public Optional<String> correctHash(long killmailId, String originalHash) {
		var today = LocalDate.now(ZoneOffset.UTC);

		for (int i = 0; i < HISTORY_DAYS; i++) {
			var date = today.minusDays(i);
			try {
				var hashes = fetchHashesForDate(date);

				if (hashes.containsKey(killmailId)) {
					var hash = hashes.get(killmailId);
					if (CCP_VERIFIED.equals(hash)) {
						log.debug("Killmail {} is CCP VERIFIED, skipping", killmailId);
						return Optional.empty();
					}
					log.debug("Found corrected hash for killmail {} from {}", killmailId, date);
					return Optional.of(hash);
				}
			} catch (Exception e) {
				log.debug("Failed to fetch hashes from Zkillboard for {}: {}", date, e.getMessage());
			}
		}

		log.debug("No corrected hash found for killmail {} in last {} days", killmailId, HISTORY_DAYS);
		return Optional.empty();
	}

	/**
	 * Fetches the killmail hash map for a specific date from Zkillboard.
	 *
	 * @param date the date to fetch
	 * @return a map of killmail ID to hash
	 */
	private Map<Long, String> fetchHashesForDate(LocalDate date) {
		return hashCache.computeIfAbsent(date, d -> {
			try {
				var dateStr = d.format(DATE_FORMATTER);
				var url = ZKILLBOARD_BASE_URL + dateStr + ".json";

				var response = okHttpWrapper.get(url);
				var body = response.body();
				if (body == null) {
					return new HashMap<>();
				}
				var json = objectMapper.readTree(body.string());

				Map<Long, String> hashes = new HashMap<>();
				if (json.isObject()) {
					var iterator = json.fields();
					while (iterator.hasNext()) {
						var entry = iterator.next();
						try {
							var killmailId = Long.parseLong(entry.getKey());
							var hashValue = entry.getValue().asText();
							hashes.put(killmailId, hashValue);
						} catch (NumberFormatException e) {
							log.debug("Skipping non-numeric killmail ID: {}", entry.getKey());
						}
					}
				}
				return hashes;
			} catch (Exception e) {
				log.debug("Failed to fetch Zkillboard data for {}: {}", date, e.getMessage());
				return new HashMap<>();
			}
		});
	}
}
