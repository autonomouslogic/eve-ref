package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class ArchivePathFactoryTest {
	@Test
	void shouldGenerateReferenceDataNames() {
		var factory = ArchivePathFactory.REFERENCE_DATA;
		assertEquals("reference-data/reference-data-latest.tar.xz", factory.createLatestPath());
		assertEquals(
				"reference-data/history/2022/reference-data-2022-01-05.tar.xz",
				factory.createArchivePath(Instant.parse("2022-01-05T04:45:02Z")));
	}

	@Test
	void shouldGenerateMarketOrderNames() {
		var factory = ArchivePathFactory.MARKET_ORDERS;
		assertEquals("market-orders/market-orders-latest.v3.csv.bz2", factory.createLatestPath());
		assertEquals(
				"market-orders/history/2022/2022-01-05/market-orders-2022-01-05_04-45-02.v3.csv.bz2",
				factory.createArchivePath(Instant.parse("2022-01-05T04:45:02Z")));
	}

	@Test
	void shouldGeneratePublicContractNames() {
		var factory = ArchivePathFactory.PUBLIC_CONTRACTS;
		assertEquals("public-contracts/public-contracts-latest.v2.tar.bz2", factory.createLatestPath());
		assertEquals(
				"public-contracts/history/2021/2021-06-20/public-contracts-2021-06-20_03-15-02.v2.tar.bz2",
				factory.createArchivePath(Instant.parse("2021-06-20T03:15:02Z")));
	}

	@Test
	void shouldGenerateKillmailsNames() {
		var factory = ArchivePathFactory.KILLMAILS;
		assertEquals(
				"killmails/2016/killmails-2016-01-04.tar.bz2",
				factory.createArchivePath(LocalDate.parse("2016-01-04")));
	}

	@Test
	void shouldGenerateMarketHistoryNames() {
		var factory = ArchivePathFactory.MARKET_HISTORY;
		assertEquals(
				"market-history/2022/market-history-2022-01-08.csv.bz2",
				factory.createArchivePath(LocalDate.parse("2022-01-08")));
	}

	@Test
	void shouldGenerateSdeNames() {
		var factory = ArchivePathFactory.SDE;
		assertEquals("ccp/sde/sde-latest-TRANQUILITY.zip", factory.createLatestPath());
		assertEquals(
				"ccp/sde/sde-20230315-TRANQUILITY.zip",
				factory.createArchivePath(Instant.parse("2023-03-15T00:00:00Z")));
	}

	@Test
	void shouldGenerateEsiNames() {
		var factory = ArchivePathFactory.ESI;
		assertEquals("esi-scrape/eve-ref-esi-scrape-latest.tar.xz", factory.createLatestPath());
		assertEquals(
				"esi-scrape/history/2023/eve-ref-esi-scrape-2023-03-15.tar.xz",
				factory.createArchivePath(Instant.parse("2023-03-15T00:00:00Z")));
	}
}
