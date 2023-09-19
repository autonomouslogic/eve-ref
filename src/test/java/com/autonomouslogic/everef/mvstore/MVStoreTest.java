package com.autonomouslogic.everef.mvstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladsch.flexmark.util.misc.Pair;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MVStoreTest {
	@Inject
	MVStoreUtil mvStoreUtil;

	@Inject
	ObjectMapper objectMapper;

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
		var obj1 = objectMapper.createObjectNode().put("v", "1");
		var obj2 = objectMapper.createObjectNode().put("v", "2");
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

	@Test
	void shouldHandleModifications() {
		var rng = new Random();
		var items = 1000;
		for (int i = 0; i < items; i++) {
			var json = objectMapper.createObjectNode().put("v", i);
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
