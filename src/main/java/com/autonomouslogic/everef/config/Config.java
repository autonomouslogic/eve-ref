package com.autonomouslogic.everef.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@Builder
public class Config<T> {
	@NonNull
	@Getter
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
		var fileName = name + "_FILE";
		var env = System.getenv();
		if (env.containsKey(name) && env.containsKey(fileName)) {
			throw new IllegalArgumentException(
					String.format("Both %s and %s cannot be set at the same time", name, fileName));
		}
		if (!env.containsKey(name) && !env.containsKey(fileName)) {
			return Optional.empty();
		}
		return getFromEnv(name).or(() -> getFromFile(fileName));
	}

	private Optional<T> getFromEnv(String env) {
		var value = System.getenv(env);
		if (value == null || value.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(parse(env, value));
	}

	@SneakyThrows
	private Optional<T> getFromFile(String env) {
		var filename = System.getenv(env);
		if (filename == null || filename.isEmpty()) {
			return Optional.empty();
		}
		var file = new File(filename);
		if (!file.exists()) {
			throw new FileNotFoundException(filename);
		}
		var value = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		value = StringUtils.trim(value);
		if (value.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(parse(env, value));
	}

	private T parse(String env, String value) {
		try {
			if (type == String.class) {
				return type.cast(value);
			}
			if (type == Integer.class) {
				return type.cast(Integer.parseInt(value));
			}
			if (type == Long.class) {
				return type.cast(Long.parseLong(value));
			}
			if (type == Float.class) {
				return type.cast(Float.parseFloat(value));
			}
			if (type == Double.class) {
				return type.cast(Double.parseDouble(value));
			}
			if (type == Boolean.class) {
				return type.cast(Boolean.parseBoolean(value));
			}
			if (type == Duration.class) {
				return type.cast(Duration.parse(value));
			}
			if (type == Period.class) {
				return type.cast(Period.parse(value));
			}
			if (type == URI.class) {
				return type.cast(new URI(value));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Unable to parse value in %s as type %s", env, type));
		}
		throw new IllegalArgumentException(String.format("Unsupported type %s for %s", type, env));
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
