package com.autonomouslogic.everef.mvstore;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.ObjectDataType;
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
				//				var id = Integer.toString(i);
				var json = (ObjectNode) map.get(id);
				json.put(Integer.toString(r), r);
				map.put(id, json);
			}
		}
	}

	@Test
	@SneakyThrows
	void modificationTestRaw() {
		var mapper = new ObjectMapper();
		var dataType = new ObjectDataType() {
			@Override
			@SneakyThrows
			public void write(WriteBuffer buff, Object obj) {
				byte[] json = mapper.writeValueAsBytes(obj);
				buff.putInt(json.length);
				buff.put(json);
			}

			@Override
			@SneakyThrows
			public Object read(ByteBuffer buff) {
				int len = buff.getInt();
				byte[] json = new byte[len];
				buff.get(json);
				return mapper.readTree(json);
			}
		};

		var file = new File("/tmp/test.db");
		file.deleteOnExit();
		var builder = new MVStore.Builder().fileName(file.getAbsolutePath());
		var store = builder.open();
		store.setVersionsToKeep(0);

		Map<Integer, JsonNode> map = store.openMap(
				"test",
				new MVMap.Builder<Integer, JsonNode>()
						.keyType(new ObjectDataType())
						.valueType(dataType));

		var items = 10000;
		var rounds = 1000;
		for (int i = 0; i < items; i++) {
			map.put(i, mapper.createObjectNode());
		}
		var rng = new Random();
		for (int r = 0; r < rounds; r++) {
			System.out.println("Round " + r);
			for (int i = 0; i < items; i++) {
				var id = rng.nextInt(items);
				var json = (ObjectNode) map.get(id);
				json.put(Integer.toString(r), r);
				map.put(id, json);
			}
		}
	}
}
