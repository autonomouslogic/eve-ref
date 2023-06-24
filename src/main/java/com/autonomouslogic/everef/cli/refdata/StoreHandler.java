package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

@RequiredArgsConstructor
public class StoreHandler {
	private final MVStoreUtil mvStoreUtil;
	private final MVStore store;

	private final Map<String, MVMap<Long, JsonNode>> maps = new ConcurrentHashMap<>();

	public MVMap<Long, JsonNode> getSdeStore(String name) {
		return getStore(name, "sde");
	}

	public MVMap<Long, JsonNode> getEsiStore(String name) {
		return getStore(name, "esi");
	}

	public MVMap<Long, JsonNode> getHoboleaksStore(String name) {
		return getStore(name, "hoboleaks");
	}

	public MVMap<Long, JsonNode> getRefStore(String name) {
		return getStore(name, "ref");
	}

	private MVMap<Long, JsonNode> getStore(String name, String type) {
		var key = name + "-" + type;
		return maps.computeIfAbsent(key, ignore -> mvStoreUtil.openJsonMap(store, key, Long.class));
	}
}
