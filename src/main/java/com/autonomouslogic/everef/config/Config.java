package com.autonomouslogic.everef.config;

import java.util.Optional;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class Config<T> {
	@NonNull
	String name;

	@NonNull
	Class<T> type;

	T defaultValue;
	Supplier<Optional<T>> defaultMethod;

	public Optional<T> get() {
		return getSetValue().or(this::getDefaultValue);
	}

	public T getRequired() {
		return get().orElseThrow(() -> new IllegalArgumentException(String.format("No value for %s", name)));
	}

	private Optional<T> getSetValue() {
		var value = System.getenv(name);
		if (value == null || value.isEmpty()) {
			return Optional.empty();
		}
		if (type == String.class) {
			return Optional.of(type.cast(value));
		}
		if (type == Integer.class) {
			return Optional.of(type.cast(Integer.parseInt(value)));
		}
		if (type == Long.class) {
			return Optional.of(type.cast(Long.parseLong(value)));
		}
		if (type == Float.class) {
			return Optional.of(type.cast(Float.parseFloat(value)));
		}
		if (type == Double.class) {
			return Optional.of(type.cast(Double.parseDouble(value)));
		}
		if (type == Boolean.class) {
			return Optional.of(type.cast(Boolean.parseBoolean(value)));
		}
		throw new IllegalArgumentException(String.format("Unsupported type %s for %s", type, name));
	}

	private Optional<T> getDefaultValue() {
		if (defaultValue != null && defaultMethod != null) {
			throw new IllegalArgumentException(
					String.format("Both default value and default method specified for %s", name));
		}
		if (defaultValue != null) {
			return Optional.of(defaultValue);
		}
		if (defaultMethod != null) {
			return defaultMethod.get();
		}
		return Optional.empty();
	}
}
