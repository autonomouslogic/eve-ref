package com.autonomouslogic.everef.cli.markethistory.scrape;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.util.LastCutoff;
import com.google.common.collect.Ordering;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RecentRegionTypeRemoverTest {
	@Inject
	LastCutoff lastCutoff;

	@Inject
	RecentRegionTypeRemover source;

	@BeforeEach
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	void shouldRemoveItemsSinceLastCutoff() {
		var cutoff = lastCutoff.getEsiRefresh().minus(Duration.ofDays(1));
		source.addHistory(MarketHistoryEntry.builder().regionId(100).typeId(10).build());
		source.addHistory(MarketHistoryEntry.builder()
				.regionId(200)
				.typeId(20)
				.httpLastModified(cutoff)
				.build());
		source.addHistory(MarketHistoryEntry.builder()
				.regionId(300)
				.typeId(30)
				.httpLastModified(cutoff.minusSeconds(1))
				.build());
		source.addHistory(MarketHistoryEntry.builder()
				.regionId(400)
				.typeId(40)
				.httpLastModified(cutoff.plusSeconds(1))
				.build());
		var result = source.sourcePairs(List.of()).toList().blockingGet();
		Collections.sort(result, Ordering.natural().onResultOf(RegionTypePair::getRegionId));
		assertEquals(List.of(new RegionTypePair(200, 20), new RegionTypePair(400, 40)), result);
	}
}
