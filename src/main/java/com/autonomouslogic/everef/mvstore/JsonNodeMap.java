package com.autonomouslogic.everef.mvstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.h2.mvstore.MVMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class JsonNodeMap<K> implements Map<K, JsonNode> {
	private final MVMap<K, byte[]> delegate;

	private final ObjectMapper objectMapper;

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object o) {
		return delegate.containsKey(o);
	}

	@Override
	public boolean containsValue(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JsonNode get(Object o) {
		throw new UnsupportedOperationException();
	}

	@Nullable
	@Override
	@SneakyThrows
	public JsonNode put(K key, JsonNode jsonNode) {
		var bytes = objectMapper.writeValueAsBytes(jsonNode);
		var r = delegate.put(key, bytes);
		return r == null ? null : objectMapper.readTree(r);
	}

	@Override
	public JsonNode remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	@SneakyThrows
	public void putAll(@NotNull Map<? extends K, ? extends JsonNode> map) {
		for (var entry : map.entrySet()) {
			var bytes = objectMapper.writeValueAsBytes(entry.getValue());
			delegate.put(entry.getKey(), bytes);
		}
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@NotNull
	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	@NotNull
	@Override
	public Collection<JsonNode> values() {
		throw new UnsupportedOperationException();
	}

	@NotNull
	@Override
	public Set<Entry<K, JsonNode>> entrySet() {
		throw new UnsupportedOperationException();
	}
}
