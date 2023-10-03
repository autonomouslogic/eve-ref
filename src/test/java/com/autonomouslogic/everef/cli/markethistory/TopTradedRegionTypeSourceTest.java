package com.autonomouslogic.everef.cli.markethistory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.math.BigDecimal;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_RATE_LIMIT_PER_S", value = "1")
@SetEnvironmentVariable(key = "MARKET_HISTORY_TOP_TRADED_MAX_TIME", value = "PT10S")
public class TopTradedRegionTypeSourceTest {
	@Inject
	TopTradedRegionTypeSource source;

	@BeforeEach
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		for (int i = 0; i < 10; i++) {
			source.addHistory(MarketHistoryEntry.builder()
					.regionId(1)
					.typeId(i)
					.average(BigDecimal.valueOf(i * 1000000))
					.volume(100)
					.build());
		}
		for (int i = 0; i < 10; i++) {
			source.addHistory(MarketHistoryEntry.builder()
					.regionId(2)
					.typeId(i + 10)
					.average(BigDecimal.valueOf(100))
					.volume(100)
					.build());
		}
	}

	@Test
	void shouldSourceItems() {
		var sourced = source.sourcePairs(List.of()).toList().blockingGet();
		assertEquals(
				List.of(
						new RegionTypePair(1, 9),
						new RegionTypePair(1, 8),
						new RegionTypePair(1, 7),
						new RegionTypePair(1, 6),
						new RegionTypePair(1, 5),
						new RegionTypePair(2, 9),
						new RegionTypePair(2, 8),
						new RegionTypePair(2, 7),
						new RegionTypePair(2, 6),
						new RegionTypePair(2, 5)),
				sourced);
	}

	@Test
	void shouldSourceItemsOutsideCurrentPairs() {
		var sourced = source.sourcePairs(List.of(new RegionTypePair(1, 9), new RegionTypePair(2, 8)))
				.toList()
				.blockingGet();
		assertEquals(
				List.of(
						new RegionTypePair(1, 8),
						new RegionTypePair(1, 7),
						new RegionTypePair(1, 6),
						new RegionTypePair(1, 5),
						new RegionTypePair(1, 4),
						new RegionTypePair(2, 9),
						new RegionTypePair(2, 7),
						new RegionTypePair(2, 6),
						new RegionTypePair(2, 5),
						new RegionTypePair(2, 4)),
				sourced);
	}
}
