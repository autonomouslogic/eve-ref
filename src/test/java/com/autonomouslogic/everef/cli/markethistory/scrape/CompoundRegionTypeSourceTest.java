package com.autonomouslogic.everef.cli.markethistory.scrape;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.model.RegionTypePair;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CompoundRegionTypeSourceTest {
	CompoundRegionTypeSource source;

	@BeforeEach
	void before() {
		source = new CompoundRegionTypeSource().setStats(new MarketHistorySourceStats());
	}

	@Test
	void shouldReturnASingleSource() {
		source.addSource(
				currentPairs -> Flowable.fromIterable(List.of(new RegionTypePair(10, 1), new RegionTypePair(20, 2))));
		var result = source.sourcePairs().toList().blockingGet();
		assertEquals(List.of(new RegionTypePair(10, 1), new RegionTypePair(20, 2)), result);
	}

	@Test
	void shouldMergeMultipleSourcesInOrder() {
		source.addSource(
				currentPairs -> Flowable.fromIterable(List.of(new RegionTypePair(10, 1), new RegionTypePair(20, 2))));
		source.addSource(
				currentPairs -> Flowable.fromIterable(List.of(new RegionTypePair(30, 3), new RegionTypePair(20, 2))));
		var result = source.sourcePairs().toList().blockingGet();
		assertEquals(List.of(new RegionTypePair(10, 1), new RegionTypePair(20, 2), new RegionTypePair(30, 3)), result);
	}

	@Test
	void shouldSubtractSources() {
		source.addSource(currentPairs -> Flowable.fromIterable(
				List.of(new RegionTypePair(10, 1), new RegionTypePair(20, 2), new RegionTypePair(30, 3))));
		source.addSource(new RegionTypeSource() {
			@Override
			public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
				return Flowable.fromIterable(List.of(new RegionTypePair(20, 2)));
			}

			@Override
			public boolean isAdditive() {
				return false;
			}
		});
		var result = source.sourcePairs().toList().blockingGet();
		assertEquals(List.of(new RegionTypePair(10, 1), new RegionTypePair(30, 3)), result);
	}
}
