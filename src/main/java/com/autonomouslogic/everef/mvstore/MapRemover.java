package com.autonomouslogic.everef.mvstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Conditionally removes entries from a Map.
 * @param <K>
 * @param <V>
 */
@RequiredArgsConstructor
public class MapRemover<K, V> {
	private final Map<K, V> map;

	@Getter
	private int entriesRemoved;

	public MapRemover<K, V> removeIf(BiPredicate<K, V> predicate) {
		List<K> keys = new ArrayList<>();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			if (predicate.test(entry.getKey(), entry.getValue())) {
				keys.add(entry.getKey());
			}
		}
		keys.forEach(map::remove);
		entriesRemoved += keys.size();
		return this;
	}
}
