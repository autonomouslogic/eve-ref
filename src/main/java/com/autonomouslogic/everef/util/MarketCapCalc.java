package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypeMarketCap;
import com.autonomouslogic.everef.model.RegionTypePair;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Calculates the market capitalisation for each type.
 */
public class MarketCapCalc {
	private final Map<RegionTypePair, BigDecimal> caps = new HashMap<>();

	@Inject
	protected MarketCapCalc() {}

	public synchronized void add(MarketHistoryEntry entry) {
		var key = RegionTypePair.fromHistory(entry);
		var cap = caps.get(key);
		if (cap == null) {
			cap = BigDecimal.ZERO;
		}
		var price = entry.getAverage();
		var volume = BigDecimal.valueOf(entry.getVolume());
		var entryCap = price.multiply(volume);
		cap = cap.add(entryCap);
		caps.put(key, cap);
	}

	public List<RegionTypeMarketCap> getRegionTypeMarketCaps() {
		return caps.entrySet().stream()
				.map(entry -> RegionTypeMarketCap.builder()
						.regionId(entry.getKey().getRegionId())
						.typeId(entry.getKey().getTypeId())
						.cap(entry.getValue())
						.build())
				.toList();
	}
}
