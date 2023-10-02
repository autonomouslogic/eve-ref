package com.autonomouslogic.everef.cli.markethistory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.model.RegionTypePair;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MarketHistorySourceStatsTest {
	@Test
	void shouldCollectAndCalculateStats() {
		var source1 = new Source1();
		var source2 = new Source2();
		var stats = new MarketHistorySourceStats();
		stats.sourceAll(
				source1,
				List.of(
						new RegionTypePair(100, 1),
						new RegionTypePair(100, 2),
						new RegionTypePair(100, 3),
						new RegionTypePair(100, 4)));

		stats.sourceAll(
				source2,
				List.of(
						new RegionTypePair(101, 1),
						new RegionTypePair(101, 2),
						new RegionTypePair(101, 3),
						new RegionTypePair(101, 4),
						new RegionTypePair(101, 5)));

		stats.hit(new RegionTypePair(100, 1));
		stats.hit(new RegionTypePair(101, 1));
		stats.hit(new RegionTypePair(101, 2));

		assertEquals(
				List.of(
						new MarketHistorySourceStats.Stat("Source1", 4, 1),
						new MarketHistorySourceStats.Stat("Source2", 5, 2)),
				stats.getStats());

		stats.logStats();
	}

	class Source1 implements RegionTypeSource {
		@Override
		public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
			return null;
		}
	}

	class Source2 implements RegionTypeSource {
		@Override
		public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
			return null;
		}
	}
}
