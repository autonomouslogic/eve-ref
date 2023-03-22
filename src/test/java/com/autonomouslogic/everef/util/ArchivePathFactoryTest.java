package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class ArchivePathFactoryTest {
	@Test
	void shouldGenerateMarketOrderNames() {
		var factory = ArchivePathFactory.marketOrders();
		assertEquals("market-orders/market-orders-latest.v3.csv.bz2", factory.createLatestPath());
		assertEquals(
				"market-orders/history/2022/2022-01-05/market-orders-2022-01-05_04-45-02.v3.csv.bz2",
				factory.createArchivePath(Instant.parse("2022-01-05T04:45:02Z")));
	}
}
