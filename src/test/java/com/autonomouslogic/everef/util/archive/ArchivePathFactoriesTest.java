package com.autonomouslogic.everef.util.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class ArchivePathFactoriesTest {
	@Test
	void shouldGenerateReferenceDataNames() {
		var factory = ArchivePathFactories.REFERENCE_DATA;
		testExpectedPaths(
				factory,
				Instant.parse("2022-01-05T00:00:00Z"),
				"reference-data/history/2022/reference-data-2022-01-05.tar.xz",
				"reference-data/reference-data-latest.tar.xz");
	}

	@Test
	void shouldGenerateMarketOrderNames() {
		var factory = ArchivePathFactories.MARKET_ORDERS;
		testExpectedPaths(
				factory,
				Instant.parse("2022-01-05T04:45:02Z"),
				"market-orders/history/2022/2022-01-05/market-orders-2022-01-05_04-45-02.v3.csv.bz2",
				"market-orders/market-orders-latest.v3.csv.bz2");
	}

	@Test
	void shouldGeneratePublicContractNames() {
		var factory = ArchivePathFactories.PUBLIC_CONTRACTS;
		testExpectedPaths(
				factory,
				Instant.parse("2021-06-20T03:15:02Z"),
				"public-contracts/history/2021/2021-06-20/public-contracts-2021-06-20_03-15-02.v2.tar.bz2",
				"public-contracts/public-contracts-latest.v2.tar.bz2");
	}

	@Test
	void shouldGenerateKillmailsNames() {
		var factory = ArchivePathFactories.KILLMAILS;
		testExpectedPaths(factory, LocalDate.parse("2016-01-04"), "killmails/2016/killmails-2016-01-04.tar.bz2");
	}

	@Test
	void shouldGenerateMarketHistoryNames() {
		var factory = ArchivePathFactories.MARKET_HISTORY;
		testExpectedPaths(
				factory, LocalDate.parse("2022-01-08"), "market-history/2022/market-history-2022-01-08.csv.bz2");
	}

	@Test
	void shouldGenerateSdeNames() {
		var factory = ArchivePathFactories.SDE;
		testExpectedPaths(
				factory,
				Instant.parse("2023-03-15T00:00:00Z"),
				"ccp/sde/2023/sde-20230315-TRANQUILITY.zip",
				"ccp/sde/sde-latest-TRANQUILITY.zip");
	}

	@Test
	void shouldGenerateStaticDataJsonlNames() {
		var factory = ArchivePathFactories.SDE_V2_JSONL;
		testExpectedPaths(
				factory,
				Instant.parse("2023-01-01T00:00:00Z"),
				"ccp/sde/2023/eve-online-static-data-jsonl.zip",
				"ccp/sde/eve-online-static-data-latest-jsonl.zip");
	}

	@Test
	void shouldGenerateStaticDataYamlNames() {
		var factory = ArchivePathFactories.SDE_V2_YAML;
		testExpectedPaths(
				factory,
				Instant.parse("2023-01-01T00:00:00Z"),
				"ccp/sde/2023/eve-online-static-data-yaml.zip",
				"ccp/sde/eve-online-static-data-latest-yaml.zip");
	}

	@Test
	void shouldGenerateEsiNames() {
		var factory = ArchivePathFactories.ESI;
		testExpectedPaths(
				factory,
				Instant.parse("2023-03-15T00:00:00Z"),
				"esi-scrape/history/2023/eve-ref-esi-scrape-2023-03-15.tar.xz",
				"esi-scrape/eve-ref-esi-scrape-latest.tar.xz");
	}

	@Test
	void shouldGenerateHoboleaksSdeNames() {
		var factory = ArchivePathFactories.HOBOLEAKS;
		testExpectedPaths(
				factory,
				Instant.parse("2023-03-15T00:00:00Z"),
				"hoboleaks-sde/history/2023/hoboleaks-sde-2023-03-15.tar.xz",
				"hoboleaks-sde/hoboleaks-sde-latest.tar.xz");
	}

	@Test
	void shouldGenerateStructuresNames() {
		var factory = ArchivePathFactories.STRUCTURES;
		testExpectedPaths(
				factory,
				Instant.parse("2023-03-15T01:02:03Z"),
				"structures/history/2023/2023-03-15/structures-2023-03-15_01-02-03.v2.json.bz2",
				"structures/structures-latest.v2.json");
	}

	@Test
	void shouldGenerateMer() {
		var factory = ArchivePathFactories.MER;
		testExpectedPaths(factory, LocalDate.parse("2026-05-01"), "ccp/mer/2026/EVEOnline_MER_202605.zip");
	}

	@Test
	void shouldGenerateMerOld1() {
		var factory = ArchivePathFactories.MER_OLD_1;
		testParsePaths(factory, LocalDate.parse("2025-04-01"), "ccp/mer/2025/EVEOnline_MER_Apr2025.zip");
	}

	@Test
	void shouldGenerateMerOld1_updated() {
		var factory = ArchivePathFactories.MER_OLD_1_UPDATED;
		testParsePaths(factory, LocalDate.parse("2022-10-01"), "ccp/mer/2022/EVEOnline_MER_Oct2022-updated.zip");
		testParsePaths(factory, LocalDate.parse("2022-09-01"), "ccp/mer/2022/EVEOnline_MER_Sep2022-Updated.zip");
		testParsePaths(factory, LocalDate.parse("2021-12-01"), "ccp/mer/2021/EVEOnline_MER_Dec2021_Updated.zip");
		testParsePaths(factory, LocalDate.parse("2019-12-01"), "ccp/mer/2019/EVEOnline_MER_Dec2019b.zip");
		testParsePaths(factory, LocalDate.parse("2016-12-01"), "ccp/mer/2016/EVEOnline_MER_Dec2016_v1.1.zip");
		testParsePaths(factory, LocalDate.parse("2024-07-01"), "ccp/mer/2024/EVEOnline_MER_Jul2024v2.zip");
	}

	@Test
	void shouldGenerateMerOld2() {
		var factory = ArchivePathFactories.MER_OLD_2;
		testParsePaths(factory, LocalDate.parse("2022-01-01"), "ccp/mer/2022/January_2022_MER.zip");
	}

	@Test
	void shouldGenerateMerOld3() {
		var factory = ArchivePathFactories.MER_OLD_3;
		testParsePaths(factory, LocalDate.parse("2021-09-01"), "ccp/mer/2021/EVEOnline_MER_Sep2021.zip");
	}

	@Test
	void shouldGenerateMerOld3FullMonth() {
		var factory = ArchivePathFactories.MER_OLD_3_FULL_MONTH;
		testParsePaths(factory, LocalDate.parse("2016-07-01"), "ccp/mer/2016/EVEOnline_MER_July2016.zip");
		testParsePaths(factory, LocalDate.parse("2016-06-01"), "ccp/mer/2016/EVEOnline_MER_June2016.zip");
	}

	private static void testExpectedPaths(
			ArchivePathFactory factory, Instant timestamp, String expectedTimePath, String expectedLatestPath) {
		assertEquals(expectedLatestPath, factory.createLatestPath());
		assertEquals(expectedTimePath, factory.createArchivePath(timestamp));

		assertEquals(timestamp, factory.parseArchiveTime(expectedTimePath));
		assertEquals(timestamp, factory.parseArchiveTime("/" + expectedTimePath));
		assertNull(factory.parseArchiveTime(expectedLatestPath));
	}

	private static void testExpectedPaths(ArchivePathFactory factory, LocalDate datestamp, String expectedTimePath) {
		assertEquals(expectedTimePath, factory.createArchivePath(datestamp));
		testParsePaths(factory, datestamp, expectedTimePath);
	}

	private static void testParsePaths(ArchivePathFactory factory, LocalDate datestamp, String expectedTimePath) {
		assertEquals(
				datestamp,
				factory.parseArchiveTime(expectedTimePath)
						.atZone(ZoneOffset.UTC)
						.toLocalDate());
	}
}
