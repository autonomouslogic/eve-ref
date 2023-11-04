package com.autonomouslogic.everef.cli.markethistory.scrape;

import com.autonomouslogic.everef.model.RegionTypePair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MarketHistorySourceStats {
	private final Set<String> order = new LinkedHashSet<>();
	private final Map<String, List<RegionTypePair>> sourced = new ConcurrentHashMap<>();
	private final Set<RegionTypePair> hits = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public void sourceAll(RegionTypeSource source, Iterable<RegionTypePair> pair) {
		pair.forEach(p -> source(source, p));
	}

	public void source(RegionTypeSource source, RegionTypePair pair) {
		var sourceName = source.getClass().getSimpleName();
		order.add(sourceName);
		sourced.computeIfAbsent(sourceName, k -> new ArrayList<>());
		sourced.computeIfPresent(sourceName, (k, v) -> {
			v.add(pair);
			return v;
		});
	}

	public void hit(RegionTypePair pair) {
		hits.add(pair);
	}

	public List<Stat> getStats() {
		return order.stream()
				.map(source -> {
					var pairs = sourced.get(source);
					var t = pairs.size();
					var h = (int) pairs.stream().filter(hits::contains).count();
					return new Stat(source, t, h);
				})
				.toList();
	}

	public void logStats() {
		getStats()
				.forEach(s -> log.info(String.format(
						"Source %s: hit %s of %s pairs - %.1f%%",
						s.getSource(), s.getHits(), s.getTotal(), 100.0 * s.getHits() / s.getTotal())));
	}

	@Value
	public static class Stat {
		String source;
		int total;
		int hits;
	}
}
