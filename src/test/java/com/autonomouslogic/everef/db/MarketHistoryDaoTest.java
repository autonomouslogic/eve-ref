package com.autonomouslogic.everef.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import io.reactivex.rxjava3.core.Flowable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log4j2
public class MarketHistoryDaoTest {
	@Inject
	DbAccess dbAccess;

	@Inject
	MarketHistoryDao marketHistoryDao;

	final MarketHistoryEntry entry = MarketHistoryEntry.builder()
			.date(LocalDate.parse("2020-01-01"))
			.regionId(10000002)
			.typeId(34)
			.average(new BigDecimal("100.00"))
			.highest(new BigDecimal("200.00"))
			.lowest(new BigDecimal("50.00"))
			.volume(1000)
			.orderCount(100)
			.httpLastModified(Instant.parse("2020-01-01T00:00:00Z"))
			.build();

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		dbAccess.flyway().clean();
		dbAccess.flyway().migrate();
	}

	@Test
	void shouldInsertAndSelectMarketHistory() {
		marketHistoryDao.insert(entry).blockingAwait();
		var fetched = marketHistoryDao
				.fetchByPK(entry.getDate(), entry.getRegionId(), entry.getTypeId())
				.blockingGet();
		assertEquals(entry, fetched);
	}

	@Test
	@SneakyThrows
	void shouldSumPresentDailyPairs() {
		var insertedDailyPairs = Map.of(
				LocalDate.parse("2020-01-01"), 1,
				LocalDate.parse("2020-01-02"), 2,
				LocalDate.parse("2020-01-03"), 3,
				LocalDate.parse("2020-01-04"), 4,
				LocalDate.parse("2020-01-05"), 5);
		var entries = Flowable.fromIterable(insertedDailyPairs.entrySet())
				.flatMap(day -> Flowable.range(0, day.getValue()).map(i -> entry.toBuilder()
						.date(day.getKey())
						.regionId(10000000 + i % 5)
						.typeId(1 + i)
						.build()))
				.toList()
				.blockingGet();
		marketHistoryDao.insert(entries).blockingAwait();
		var dailyPairs =
				marketHistoryDao.fetchDailyPairs(LocalDate.parse("2020-01-02")).blockingGet();
		var expectedDailyPairs = new HashMap<>(insertedDailyPairs);
		expectedDailyPairs.remove(LocalDate.parse("2020-01-01"));
		assertEquals(expectedDailyPairs, dailyPairs);
	}
}
