package com.autonomouslogic.everef.mvstore;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Random;
import javax.inject.Inject;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MVStoreTest {
	@Inject
	MVStoreUtil mvStoreUtil;

	@Inject
	ObjectMapper objectMapper;

	MVStore store;
	MVMap<String, JsonNode> map;

	@BeforeEach
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		store = mvStoreUtil.createTempStore(MVStoreTest.class.getSimpleName());
		map = mvStoreUtil.openJsonMap(store, "test", String.class);
	}

	@Test
	void shouldHandleManyModifications() {
		var rng = new Random();
		var items = 10000;
		var rounds = 100;
		for (int i = 0; i < items; i++) {
			var json = objectMapper.createObjectNode().put("v", rng.nextInt());
			map.put(Integer.toString(i), json);
		}
		for (int r = 0; r < rounds; r++) {
			System.out.println("Round " + r);
			for (int i = 0; i < items; i++) {
				var id = Integer.toString(rng.nextInt(items));
				var json = (ObjectNode) map.get(id);
				json.put("v" + r, rng.nextInt());
				map.put(id, json);
			}
		}
	}
}
