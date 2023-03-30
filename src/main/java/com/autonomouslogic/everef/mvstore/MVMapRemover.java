package com.autonomouslogic.everef.mvstore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.h2.mvstore.MVMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Conditionally removed entries from an MVMap.
 * @param <K>
 * @param <V>
 */
@RequiredArgsConstructor
public class MVMapRemover<K, V> {
	private final MVMap<K, V> map;
	@Getter
	private int entriesRemoved;

	public MVMapRemover<K, V> removeIf(BiPredicate<K, V> predicate) {
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
