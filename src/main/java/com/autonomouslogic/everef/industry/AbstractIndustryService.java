package com.autonomouslogic.everef.industry;

import com.autonomouslogic.commons.ResourceUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;

public abstract class AbstractIndustryService<T> {
	private final Map<Long, T> entries;

	@SneakyThrows
	protected AbstractIndustryService(Class<T> clazz, String file, Function<T, Long> idGetter, CsvMapper csvMapper) {
		var schema = csvMapper
				.schemaFor(clazz)
				.withStrictHeaders(true)
				.withColumnReordering(true)
				.withHeader();
		MappingIterator<T> iterator = null;
		try {
			iterator = csvMapper.readerFor(clazz).with(schema).readValues(ResourceUtil.loadResource(file));
			entries = new HashMap<>();
			iterator.forEachRemaining(entry -> {
				entries.put(idGetter.apply(entry), entry);
			});
		} finally {
			if (iterator != null) {
				iterator.close();
			}
		}
	}

	public T get(long id) {
		return entries.get(id);
	}

	public Stream<T> stream() {
		return entries.values().stream();
	}
}
