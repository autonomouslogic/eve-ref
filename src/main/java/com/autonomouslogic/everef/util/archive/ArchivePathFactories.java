package com.autonomouslogic.everef.util.archive;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArchivePathFactories {
	public static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
	public static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	public static final DateTimeFormatter COMPACT_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

	private static final List<ArchivePathFactory> allPatterns = new ArrayList<>();

	public static final ArchivePathFactory HOBOLEAKS = register(ArchivePathFactory.builder()
			.folder("hoboleaks-sde")
			.filename("hoboleaks-sde")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build());
	public static final ArchivePathFactory ESI = register(ArchivePathFactory.builder()
			.folder("esi-scrape")
			.filename("eve-ref-esi-scrape")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build());
	public static final ArchivePathFactory MARKET_HISTORY = register(ArchivePathFactory.builder()
			.folder("market-history")
			.filename("market-history")
			.historyFolder(false)
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".csv.bz2")
			.build());
	public static final ArchivePathFactory KILLMAILS = register(ArchivePathFactory.builder()
			.folder("killmails")
			.filename("killmails")
			.historyFolder(false)
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.bz2")
			.build());
	public static final ArchivePathFactory REFERENCE_DATA = register(ArchivePathFactory.builder()
			.folder("reference-data")
			.filename("reference-data")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build());
	public static final ArchivePathFactory SDE = register(ArchivePathFactory.builder()
			.name("sde")
			.folder("ccp/sde")
			.filename("sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(COMPACT_DATE_PATTERN)
			.suffix("-TRANQUILITY.zip")
			.build());
	public static final ArchivePathFactory MARKET_ORDERS = register(ArchivePathFactory.builder()
			.folder("market-orders")
			.filename("market-orders")
			.suffix(".v3.csv.bz2")
			.build());
	public static final ArchivePathFactory PUBLIC_CONTRACTS = register(ArchivePathFactory.builder()
			.folder("public-contracts")
			.filename("public-contracts")
			.suffix(".v2.tar.bz2")
			.build());
	public static final ArchivePathFactory FREELANCE_JOBS = register(ArchivePathFactory.builder()
			.folder("freelance-jobs")
			.filename("freelance-jobs")
			.suffix(".json.bz2")
			.latestSuffix(".json.bz2")
			.build());
	public static final ArchivePathFactory SDE_V2_JSONL = register(ArchivePathFactory.builder()
			.name("sde-v2-jsonl")
			.folder("ccp/sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.filename("eve-online-static-data")
			.fileDateTimeFormatter(null)
			.suffix("-jsonl.zip")
			.build());
	public static final ArchivePathFactory SDE_V2_YAML = register(ArchivePathFactory.builder()
			.name("sde-v2-yaml")
			.folder("ccp/sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.filename("eve-online-static-data")
			.fileDateTimeFormatter(null)
			.suffix("-yaml.zip")
			.build());
	public static final ArchivePathFactory FUZZWORK_ORDERSET = register(ArchivePathFactory.builder()
			.name("fuzzwork-ordersets")
			.folder("fuzzwork/ordersets")
			.historyFolder(false)
			.filename("fuzzwork-orderset-")
			.suffix(".csv.gz")
			.build());
	public static final ArchivePathFactory STRUCTURES = register(ArchivePathFactory.builder()
			.folder("structures")
			.filename("structures")
			.suffix(".v2.json.bz2")
			.latestSuffix(".v2.json")
			.build());
	public static final ArchivePathFactory SOVEREIGNTY_MAP = register(ArchivePathFactory.builder()
			.folder("sovereignty-map")
			.filename("sovereignty-map")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory SOVEREIGNTY_STRUCTURES = register(ArchivePathFactory.builder()
			.folder("sovereignty-structures")
			.filename("sovereignty-structures")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory SOVEREIGNTY_CAMPAIGNS = register(ArchivePathFactory.builder()
			.folder("sovereignty-campaigns")
			.filename("sovereignty-campaigns")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory INDUSTRY_SYSTEMS = register(ArchivePathFactory.builder()
			.folder("industry-systems")
			.filename("industry-systems")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory INDUSTRY_FACILITIES = register(ArchivePathFactory.builder()
			.folder("industry-facilities")
			.filename("industry-facilities")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	// System Stats (2)
	public static final ArchivePathFactory SYSTEM_JUMPS = register(ArchivePathFactory.builder()
			.folder("system-jumps")
			.filename("system-jumps")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory SYSTEM_KILLS = register(ArchivePathFactory.builder()
			.folder("system-kills")
			.filename("system-kills")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory WARZONE = register(ArchivePathFactory.builder()
			.folder("warzone")
			.filename("warzone")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory WARZONE_INSURGENCY = register(ArchivePathFactory.builder()
			.folder("warzone-insurgency")
			.filename("warzone-insurgency")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory WARZONE_LEADERBOARD = register(ArchivePathFactory.builder()
			.folder("warzone-leaderboard")
			.filename("warzone-leaderboard")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory INSURANCE_PRICES = register(ArchivePathFactory.builder()
			.folder("insurance-prices")
			.filename("insurance-prices")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory UNIVERSE_STRUCTURES = register(ArchivePathFactory.builder()
			.folder("universe-structures")
			.filename("universe-structures")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory FACTION_WARFARE_STATS = register(ArchivePathFactory.builder()
			.folder("faction-warfare-stats")
			.filename("faction-warfare-stats")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory FACTION_WARFARE_WARS = register(ArchivePathFactory.builder()
			.folder("faction-warfare-wars")
			.filename("faction-warfare-wars")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory FACTION_WARFARE_LEADERBOARDS = register(ArchivePathFactory.builder()
			.folder("faction-warfare-leaderboards")
			.filename("faction-warfare-leaderboards")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory FACTION_WARFARE_LEADERBOARDS_CHARACTERS =
			register(ArchivePathFactory.builder()
					.folder("faction-warfare-leaderboards-characters")
					.filename("faction-warfare-leaderboards-characters")
					.suffix(".json.bz2")
					.latestSuffix(".json")
					.build());
	public static final ArchivePathFactory FACTION_WARFARE_LEADERBOARDS_CORPORATIONS =
			register(ArchivePathFactory.builder()
					.folder("faction-warfare-leaderboards-corporations")
					.filename("faction-warfare-leaderboards-corporations")
					.suffix(".json.bz2")
					.latestSuffix(".json")
					.build());
	public static final ArchivePathFactory FACTION_WARFARE_SYSTEMS = register(ArchivePathFactory.builder()
			.folder("faction-warfare-systems")
			.filename("faction-warfare-systems")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory INCURSIONS = register(ArchivePathFactory.builder()
			.folder("incursions")
			.filename("incursions")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory MARKETS_PRICES = register(ArchivePathFactory.builder()
			.folder("markets-prices")
			.filename("markets-prices")
			.suffix(".json.bz2")
			.latestSuffix(".json")
			.build());
	public static final ArchivePathFactory MER = register(ArchivePathFactory.builder()
			.name("mer")
			.folder("ccp/mer")
			.filename("EVEOnline_MER")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(
					new DateTimeFormatterBuilder().appendPattern("yyyyMM").toFormatter())
			.dateFormatterSeparator("_")
			.suffix(".zip")
			.build());
	public static final ArchivePathFactory MER_OLD_1 = register(ArchivePathFactory.builder()
			.name("mer")
			.folder("ccp/mer")
			.filename("EVEOnline_MER")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(
					new DateTimeFormatterBuilder().appendPattern("MMMyyyy").toFormatter())
			.dateFormatterSeparator("_")
			.suffix(".zip")
			.build());
	public static final ArchivePathFactory MER_OLD_1_UPDATED = register(ArchivePathFactory.builder()
			.name("mer")
			.folder("ccp/mer")
			.filename("EVEOnline_MER")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(
					new DateTimeFormatterBuilder().appendPattern("MMMyyyy").toFormatter())
			.dateFormatterSeparator("_")
			.suffix("-updated.zip")
			.build());
	public static final ArchivePathFactory MER_OLD_2 = register(ArchivePathFactory.builder()
			.name("mer")
			.folder("ccp/mer")
			.filename("")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(new DateTimeFormatterBuilder()
					.appendPattern("MMMM_yyyy'_MER'")
					.toFormatter())
			.dateFormatterSeparator("")
			.suffix(".zip")
			.build());
	public static final ArchivePathFactory MER_OLD_3 = register(ArchivePathFactory.builder()
			.name("mer")
			.folder("ccp/mer")
			.filename("EVEOnline_MER")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(
					new DateTimeFormatterBuilder().appendPattern("MMMyyyy").toFormatter())
			.dateFormatterSeparator("_")
			.suffix(".zip")
			.build());
	public static final ArchivePathFactory MER_OLD_3_FULL_MONTH = register(ArchivePathFactory.builder()
			.name("mer")
			.folder("ccp/mer")
			.filename("EVEOnline_MER")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(
					new DateTimeFormatterBuilder().appendPattern("MMMMyyyy").toFormatter())
			.dateFormatterSeparator("_")
			.suffix(".zip")
			.build());

	private static ArchivePathFactory register(ArchivePathFactory pattern) {
		allPatterns.add(pattern);
		return pattern;
	}

	public static Optional<ArchivePathFactory.ArchiveMatch> tryMatch(String path) {
		for (var pattern : allPatterns) {
//			if (path.startsWith(pattern.getFolder() + "/") || path.startsWith(pattern.getFolder())) {
				var name = pattern.getName();
				if (name == null) {
					name = pattern.getFolder();
				}
				var latest = pattern.createLatestPath();
				if (latest.equals(path)) {
					return Optional.of(new ArchivePathFactory.ArchiveMatch(name, Optional.empty()));
				}
				var timestamp = pattern.parseArchiveTime(path);
				if (timestamp != null) {
					return Optional.of(new ArchivePathFactory.ArchiveMatch(name, Optional.of(timestamp)));
				}
//			}
		}
		return Optional.empty();
	}
}
