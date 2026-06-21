package com.autonomouslogic.everef.util.archive;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArchivePathFactories {
	public static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
	public static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	public static final DateTimeFormatter COMPACT_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

	private static final List<ArchivePathFactory> allPatterns = new ArrayList<>();

	public static final StandardArchivePathFactory HOBOLEAKS = register(StandardArchivePathFactory.builder()
			.folder("hoboleaks-sde")
			.filename("hoboleaks-sde")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build());

	public static final StandardArchivePathFactory ESI = register(StandardArchivePathFactory.builder()
			.folder("esi-scrape")
			.filename("eve-ref-esi-scrape")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build());

	public static final StandardArchivePathFactory MARKET_HISTORY = register(StandardArchivePathFactory.builder()
			.folder("market-history")
			.filename("market-history")
			.historyFolder(false)
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".csv.bz2")
			.build());

	public static final StandardArchivePathFactory KILLMAILS = register(StandardArchivePathFactory.builder()
			.folder("killmails")
			.filename("killmails")
			.historyFolder(false)
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.bz2")
			.build());

	public static final StandardArchivePathFactory REFERENCE_DATA = register(StandardArchivePathFactory.builder()
			.folder("reference-data")
			.filename("reference-data")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build());

	public static final StandardArchivePathFactory SDE = register(StandardArchivePathFactory.builder()
			.name("sde")
			.folder("ccp/sde")
			.filename("sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(COMPACT_DATE_PATTERN)
			.suffix("-TRANQUILITY.zip")
			.build());

	public static final StandardArchivePathFactory MARKET_ORDERS = register(StandardArchivePathFactory.builder()
			.folder("market-orders")
			.filename("market-orders")
			.suffix(".v3.csv.bz2")
			.build());

	public static final StandardArchivePathFactory PUBLIC_CONTRACTS = register(StandardArchivePathFactory.builder()
			.folder("public-contracts")
			.filename("public-contracts")
			.suffix(".v2.tar.bz2")
			.build());

	public static final StandardArchivePathFactory FREELANCE_JOBS = register(StandardArchivePathFactory.builder()
			.folder("freelance-jobs")
			.filename("freelance-jobs")
			.suffix(".json.bz2")
			.latestSuffix(".json.bz2")
			.build());
	public static final StandardArchivePathFactory SDE_V2_JSONL = register(StandardArchivePathFactory.builder()
			.name("sde-v2-jsonl")
			.folder("ccp/sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.filename("eve-online-static-data")
			.fileDateTimeFormatter(null)
			.suffix("-jsonl.zip")
			.build());

	public static final StandardArchivePathFactory SDE_V2_YAML = register(StandardArchivePathFactory.builder()
			.name("sde-v2-yaml")
			.folder("ccp/sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.filename("eve-online-static-data")
			.fileDateTimeFormatter(null)
			.suffix("-yaml.zip")
			.build());

	public static final FuzzworkOrdersetArchivePathFactory FUZZWORK_ORDERSET =
			register(new FuzzworkOrdersetArchivePathFactory());

	public static final StandardArchivePathFactory STRUCTURES = register(StandardArchivePathFactory.builder()
			.folder("structures")
			.filename("structures")
			.suffix(".v2.json.bz2")
			.latestSuffix(".v2.json")
			.build());

	public static final StandardArchivePathFactory SOVEREIGNTY_MAP = register(StandardArchivePathFactory.builder()
			.folder("sovereignty-map")
			.filename("sovereignty-map")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory SOVEREIGNTY_STRUCTURES =
			register(StandardArchivePathFactory.builder()
					.folder("sovereignty-structures")
					.filename("sovereignty-structures")
					.suffix(".json.bz2")
					.latestSuffix(".json")
					.build());

	public static final StandardArchivePathFactory SOVEREIGNTY_CAMPAIGNS = register(StandardArchivePathFactory.builder()
			.folder("sovereignty-campaigns")
			.filename("sovereignty-campaigns")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory INDUSTRY_SYSTEMS = register(StandardArchivePathFactory.builder()
			.folder("industry-systems")
			.filename("industry-systems")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory INDUSTRY_FACILITIES = register(StandardArchivePathFactory.builder()
			.folder("industry-facilities")
			.filename("industry-facilities")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory SYSTEM_JUMPS = register(StandardArchivePathFactory.builder()
			.folder("system-jumps")
			.filename("system-jumps")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory SYSTEM_KILLS = register(StandardArchivePathFactory.builder()
			.folder("system-kills")
			.filename("system-kills")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory WARZONE = register(StandardArchivePathFactory.builder()
			.folder("warzone")
			.filename("warzone")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory WARZONE_INSURGENCY = register(StandardArchivePathFactory.builder()
			.folder("warzone-insurgency")
			.filename("warzone-insurgency")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory WARZONE_LEADERBOARD = register(StandardArchivePathFactory.builder()
			.folder("warzone-leaderboard")
			.filename("warzone-leaderboard")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory INSURANCE_PRICES = register(StandardArchivePathFactory.builder()
			.folder("insurance-prices")
			.filename("insurance-prices")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory UNIVERSE_STRUCTURES = register(StandardArchivePathFactory.builder()
			.folder("universe-structures")
			.filename("universe-structures")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory FACTION_WARFARE_STATS = register(StandardArchivePathFactory.builder()
			.folder("faction-warfare-stats")
			.filename("faction-warfare-stats")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory FACTION_WARFARE_WARS = register(StandardArchivePathFactory.builder()
			.folder("faction-warfare-wars")
			.filename("faction-warfare-wars")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory FACTION_WARFARE_LEADERBOARDS =
			register(StandardArchivePathFactory.builder()
					.folder("faction-warfare-leaderboards")
					.filename("faction-warfare-leaderboards")
					.suffix(".json.bz2")
					.latestSuffix(".json")
					.build());

	public static final StandardArchivePathFactory FACTION_WARFARE_LEADERBOARDS_CHARACTERS =
			register(StandardArchivePathFactory.builder()
					.folder("faction-warfare-leaderboards-characters")
					.filename("faction-warfare-leaderboards-characters")
					.suffix(".json.bz2")
					.latestSuffix(".json")
					.build());

	public static final StandardArchivePathFactory FACTION_WARFARE_LEADERBOARDS_CORPORATIONS =
			register(StandardArchivePathFactory.builder()
					.folder("faction-warfare-leaderboards-corporations")
					.filename("faction-warfare-leaderboards-corporations")
					.suffix(".json.bz2")
					.latestSuffix(".json")
					.build());

	public static final StandardArchivePathFactory FACTION_WARFARE_SYSTEMS =
			register(StandardArchivePathFactory.builder()
					.folder("faction-warfare-systems")
					.filename("faction-warfare-systems")
					.suffix(".json.bz2")
					.latestSuffix(".json")
					.build());

	public static final StandardArchivePathFactory INCURSIONS = register(StandardArchivePathFactory.builder()
			.folder("incursions")
			.filename("incursions")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final StandardArchivePathFactory MARKETS_PRICES = register(StandardArchivePathFactory.builder()
			.folder("markets-prices")
			.filename("markets-prices")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());

	public static final MerArchivePathFactory MER = register(new MerArchivePathFactory());

	private static <T extends ArchivePathFactory> T register(T pattern) {
		// Only register once - MER and its aliases point to the same instance
		if (!allPatterns.contains(pattern)) {
			allPatterns.add(pattern);
		}
		return pattern;
	}

	public static Optional<ArchiveMatch> tryMatch(String path) {
		for (var pattern : allPatterns) {
			var name = pattern.getName();
			if (name == null) {
				name = pattern.getFolder();
			}
			var latest = pattern.createLatestPath();
			if (latest.equals(path)) {
				return Optional.of(new ArchiveMatch(name, Optional.empty()));
			}
			var timestamp = pattern.parseArchiveTime(path);
			if (timestamp != null) {
				var sequence = pattern.parseSequenceNumber(path);
				return Optional.of(new ArchiveMatch(name, Optional.of(timestamp), Optional.ofNullable(sequence)));
			}
			// Also check for patterns that have sequence but no timestamp (e.g., backfill data)
			var sequence = pattern.parseSequenceNumber(path);
			if (sequence != null) {
				return Optional.of(new ArchiveMatch(name, Optional.empty(), Optional.of(sequence)));
			}
		}
		return Optional.empty();
	}
}
