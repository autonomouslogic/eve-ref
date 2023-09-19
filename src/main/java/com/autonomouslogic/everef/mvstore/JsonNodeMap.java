package com.autonomouslogic.everef.mvstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.h2.mvstore.MVMap;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class JsonNodeMap<K> extends AbstractMap<K, JsonNode> {
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
	@SneakyThrows
	public JsonNode get(Object o) {
		var bytes = delegate.get(o);
		return bytes == null ? null : objectMapper.readTree(bytes);
	}

	@Override
	@SneakyThrows
	public JsonNode put(K key, JsonNode jsonNode) {
		var bytes = objectMapper.writeValueAsBytes(jsonNode);
		var r = delegate.put(key, bytes);
		return r == null ? null : objectMapper.readTree(r);
	}

	@Override
	@SneakyThrows
	public JsonNode remove(Object o) {
		var bytes = delegate.remove(o);
		return bytes == null ? null : objectMapper.readTree(bytes);
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

	@Override
	public Set<Entry<K, JsonNode>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<K, JsonNode>> iterator() {
				return new Iterator<>() {
					private final Iterator<Entry<K, byte[]>> delegate =
							JsonNodeMap.this.delegate.entrySet().iterator();

					@Override
					public boolean hasNext() {
						return delegate.hasNext();
					}

					@Override
					@SneakyThrows
					public Entry<K, JsonNode> next() {
						var next = delegate.next();
						return new SimpleEntry<>(next.getKey(), objectMapper.readTree(next.getValue()));
					}
				};
			}

			@Override
			public int size() {
				return delegate.size();
			}
		};
	}
}
