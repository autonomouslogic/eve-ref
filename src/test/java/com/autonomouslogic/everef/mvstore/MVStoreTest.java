package com.autonomouslogic.everef.mvstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public class MVStoreTest {
	@Inject
	MVStoreUtil mvStoreUtil;

	@Inject
	JsonMapper jsonMapper;

	MVStore store;
	Map<String, JsonNode> map;

	@BeforeEach
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		store = mvStoreUtil.createTempStore(MVStoreTest.class.getSimpleName());
		map = mvStoreUtil.openJsonMap(store, "test", String.class);
	}

	@Test
	void shouldHandleValues() {
		var obj1 = jsonMapper.createObjectNode().put("v", "1");
		var obj2 = jsonMapper.createObjectNode().put("v", "2");
		map.put("a", obj1);
		map.put("b", obj2);
		assertEquals(obj1, map.get("a"));
		assertNotSame(obj1, map.get("a"));
		assertTrue(map.containsKey("a"));
		assertFalse(map.containsKey("z"));
		assertEquals(
				Set.of(Pair.of("a", obj1), Pair.of("b", obj2)),
				map.entrySet().stream()
						.map(e -> Pair.of(e.getKey(), e.getValue()))
						.collect(Collectors.toSet()));
		assertEquals(Set.of("a", "b"), new HashSet<>(map.keySet()));
		assertEquals(Set.of(obj1, obj2), new HashSet<>(map.values()));
		assertEquals(obj1, map.remove("a"));
	}

	/**
	 * Reproduces the intermittent <code>MVStoreException: Chunk N not found</code> seen in
	 * <code>ScrapeMarketHistory.uploadArchive</code>.
	 *
	 * <p>Production sequence: many entries are overwritten (leaving dead pages), then a long-lived
	 * cursor iterates a map while the store's background thread compacts. With
	 * <code>setVersionsToKeep(0)</code> and the retention window expired, compaction reclaims chunks
	 * still referenced by the open cursor. The retention time is set to zero here to avoid having to
	 * run the test for 45+ seconds; compaction is invoked explicitly instead of waiting for the
	 * background housekeeping to simulate it happening mid-iteration.
	 */
	@Test
	void shouldNotFailIterationWhileStoreCompacts() {
		store.setRetentionTime(0);
		var items = 20_000;
		var payload = "x".repeat(1000);
		for (int i = 0; i < items; i++) {
			map.put(Integer.toString(i), jsonMapper.createObjectNode().put("v", payload + i));
		}
		// Overwrite everything to create dead pages, making chunks eligible for compaction.
		for (int i = 0; i < items; i++) {
			map.put(Integer.toString(i), jsonMapper.createObjectNode().put("v", i + payload));
		}
		store.commit();
		var iterated = 0;
		for (var it = map.entrySet().iterator(); it.hasNext(); ) {
			it.next();
			iterated++;
			if (iterated % 1000 == 0) {
				// Simulates the background housekeeping running while the cursor is open.
				store.commit();
				store.compact(95, 16 * 1024 * 1024);
			}
		}
		assertEquals(items, iterated);
	}

	@Test
	void shouldHandleModifications() {
		var rng = new Random();
		var items = 1000;
		for (int i = 0; i < items; i++) {
			var json = jsonMapper.createObjectNode().put("v", i);
			map.put(Integer.toString(i), json);
		}
		var start = Instant.now();
		var x = 0;
		while (Duration.between(start, Instant.now()).compareTo(Duration.ofSeconds(3)) < 0) {
			for (int i = 0; i < 1000; i++) {
				var id = Integer.toString(rng.nextInt(items));
				var json = (ObjectNode) map.get(id);
				json.put(Integer.toString(x), x);
				map.put(id, json);
				x++;
			}
		}
	}
}
