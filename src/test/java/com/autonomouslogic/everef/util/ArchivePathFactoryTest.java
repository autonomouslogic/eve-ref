package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class ArchivePathFactoryTest {
	@Test
	void shouldGenerateReferenceDataNames() {
		var factory = ArchivePathFactory.REFERENCE_DATA;
		testExpectedPaths(
				factory,
				Instant.parse("2022-01-05T00:00:00Z"),
				"reference-data/history/2022/reference-data-2022-01-05.tar.xz",
				"reference-data/reference-data-latest.tar.xz");
	}

	@Test
	void shouldGenerateMarketOrderNames() {
		var factory = ArchivePathFactory.MARKET_ORDERS;
		testExpectedPaths(
				factory,
				Instant.parse("2022-01-05T04:45:02Z"),
				"market-orders/history/2022/2022-01-05/market-orders-2022-01-05_04-45-02.v3.csv.bz2",
				"market-orders/market-orders-latest.v3.csv.bz2");
	}

	@Test
	void shouldGeneratePublicContractNames() {
		var factory = ArchivePathFactory.PUBLIC_CONTRACTS;
		testExpectedPaths(
				factory,
				Instant.parse("2021-06-20T03:15:02Z"),
				"public-contracts/history/2021/2021-06-20/public-contracts-2021-06-20_03-15-02.v2.tar.bz2",
				"public-contracts/public-contracts-latest.v2.tar.bz2");
	}

	@Test
	void shouldGenerateKillmailsNames() {
		var factory = ArchivePathFactory.KILLMAILS;
		testExpectedPaths(factory, LocalDate.parse("2016-01-04"), "killmails/2016/killmails-2016-01-04.tar.bz2");
	}

	@Test
	void shouldGenerateMarketHistoryNames() {
		var factory = ArchivePathFactory.MARKET_HISTORY;
		testExpectedPaths(
				factory, LocalDate.parse("2022-01-08"), "market-history/2022/market-history-2022-01-08.csv.bz2");
	}

	@Test
	void shouldGenerateSdeNames() {
		var factory = ArchivePathFactory.SDE;
		testExpectedPaths(
				factory,
				Instant.parse("2023-03-15T00:00:00Z"),
				"ccp/sde/sde-20230315-TRANQUILITY.zip",
				"ccp/sde/sde-latest-TRANQUILITY.zip");
	}

	@Test
	void shouldGenerateEsiNames() {
		var factory = ArchivePathFactory.ESI;
		testExpectedPaths(
				factory,
				Instant.parse("2023-03-15T00:00:00Z"),
				"esi-scrape/history/2023/eve-ref-esi-scrape-2023-03-15.tar.xz",
				"esi-scrape/eve-ref-esi-scrape-latest.tar.xz");
	}

	private static void testExpectedPaths(
			ArchivePathFactory factory, Instant timestamp, String expectedTimePath, String expectedLatestPath) {
		assertEquals(expectedLatestPath, factory.createLatestPath());
		assertEquals(expectedTimePath, factory.createArchivePath(timestamp));

		assertEquals(timestamp, factory.parseArchiveTime(expectedTimePath));
		assertNull(factory.parseArchiveTime(expectedLatestPath));
	}

	private static void testExpectedPaths(ArchivePathFactory factory, LocalDate datestamp, String expectedTimePath) {
		assertEquals(expectedTimePath, factory.createArchivePath(datestamp));
	}
}
