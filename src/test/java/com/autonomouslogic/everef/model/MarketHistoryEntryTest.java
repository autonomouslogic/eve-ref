package com.autonomouslogic.everef.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MarketHistoryEntryTest {
	@Inject
	CsvMapper csvMapper;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	@SneakyThrows
	void shouldParseCsv() {
		var csv =
				"""
			average,date,highest,lowest,order_count,volume,region_id,type_id,http_last_modified
			50.05,2022-05-11,50.05,50.05,2,190,10000001,20,2022-05-12T11:05:40Z
			""";
		var schema = csvMapper
				.schemaFor(MarketHistoryEntry.class)
				.withHeader()
				.withStrictHeaders(true)
				.withColumnReordering(true);
		MappingIterator<MarketHistoryEntry> iterator =
				csvMapper.readerFor(MarketHistoryEntry.class).with(schema).readValues(csv);
		var entry = iterator.next();
		assertEquals(
				MarketHistoryEntry.builder()
						.average(new BigDecimal("50.05"))
						.date(LocalDate.parse("2022-05-11"))
						.highest(new BigDecimal("50.05"))
						.lowest(new BigDecimal("50.05"))
						.orderCount(2)
						.volume(190)
						.regionId(10000001)
						.typeId(20)
						.httpLastModified(Instant.parse("2022-05-12T11:05:40Z"))
						.build(),
				entry);
	}
}
