package com.autonomouslogic.everef.mvstore;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.functions.BiConsumer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

@Log4j2
public class StoreMapSet {
	@Inject
	protected MVStoreUtil mvStoreUtil;

	@NonNull
	@Setter
	private MVStore mvStore;

	private final Map<String, MVMap<String, JsonNode>> maps = new ConcurrentHashMap<>();

	@Inject
	protected StoreMapSet() {}

	public boolean hasMap(String name) {
		return maps.containsKey(name);
	}

	public Set<String> getMapNames() {
		return maps.keySet();
	}

	public void put(String map, String key, JsonNode value) {
		getOrCreateMap(map).put(key, value);
	}

	public MVMap<String, JsonNode> getOrCreateMap(String name) {
		return maps.computeIfAbsent(name, ignore -> mvStoreUtil.openJsonMap(mvStore, name, String.class));
	}

	@SneakyThrows
	public void forEachMap(BiConsumer<String, Map<String, JsonNode>> consumer) {
		for (String key : maps.keySet()) {
			consumer.accept(key, maps.get(key));
		}
	}
}
