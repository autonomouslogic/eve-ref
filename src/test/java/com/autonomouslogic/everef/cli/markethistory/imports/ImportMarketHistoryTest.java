package com.autonomouslogic.everef.cli.markethistory.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.db.MarketHistoryDao;
import com.autonomouslogic.everef.db.schema.Tables;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <ul>
 *     <li>2018-12-01 present in 2018 year file, present in database, should not be imported</li>
 *     <li>2018-12-02 present in 2018 year file, not present in database, should be imported</li>
 *     <li>2019-01-01 not present in totals.json, present in database, should not be imported</li>
 *     <li>2019-01-02 not present in totals.json, not present in database, should be imported</li>
 *     <li>2019-01-03 present in totals.json with more pairs than in database, should be imported</li>
 *     <li>2019-01-04 present in totals.json with same pairs as database, should not be imported</li>
 *     <li>2019-01-05 present in totals.json, not present in database, should be imported</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/data/")
@SetEnvironmentVariable(key = "IMPORT_MARKET_HISTORY_MIN_DATE", value = "2018-01-01")
public class ImportMarketHistoryTest {
	private static final BigDecimal IMPORTED = new BigDecimal("100.00");
	private static final BigDecimal NOT_IMPORTED = new BigDecimal("1000.00");
	private static final int REGION_ID = 10000001;

	@Inject
	DbAccess dbAccess;

	@Inject
	ImportMarketHistory importMarketHistory;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	MarketHistoryDao marketHistoryDao;

	@Inject
	TestDataUtil testDataUtil;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);

		dbAccess.flyway().clean();
		dbAccess.flyway().migrate();
		dbAccess.context().truncate(Tables.MARKET_HISTORY).execute();

		var entry = MarketHistoryEntry.builder()
				.typeId(20)
				.regionId(REGION_ID)
				.average(NOT_IMPORTED)
				.lowest(BigDecimal.ZERO)
				.highest(BigDecimal.ZERO)
				.orderCount(1)
				.httpLastModified(Instant.EPOCH)
				.build();

		marketHistoryDao
				.insert(List.of(
						entry.toBuilder().date(LocalDate.parse("2018-12-01")).build(),
						entry.toBuilder().date(LocalDate.parse("2019-01-01")).build(),
						entry.toBuilder().date(LocalDate.parse("2019-01-03")).build(),
						entry.toBuilder().date(LocalDate.parse("2019-01-04")).build(),
						entry.toBuilder()
								.date(LocalDate.parse("2019-01-04"))
								.typeId(21)
								.build()))
				.blockingAwait();
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldImportMarketHistory() {
		importMarketHistory.run().blockingAwait();
		assertDailyImports();
		assertYearlyImports();
	}

	private void assertDailyImports() {
		assertEquals(
				NOT_IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2019-01-01"), REGION_ID, 20)
						.blockingGet()
						.getAverage());

		assertEquals(
				IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2019-01-02"), REGION_ID, 20)
						.blockingGet()
						.getAverage());
		assertEquals(
				IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2019-01-02"), REGION_ID, 21)
						.blockingGet()
						.getAverage());

		assertEquals(
				NOT_IMPORTED,
				marketHistoryDao // ignored due to "on duplicate key ignore" clause
						.fetchByPK(LocalDate.parse("2019-01-03"), REGION_ID, 20)
						.blockingGet()
						.getAverage());
		assertEquals(
				IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2019-01-03"), REGION_ID, 21)
						.blockingGet()
						.getAverage());

		assertEquals(
				NOT_IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2019-01-04"), REGION_ID, 20)
						.blockingGet()
						.getAverage());
		assertEquals(
				NOT_IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2019-01-04"), REGION_ID, 21)
						.blockingGet()
						.getAverage());

		assertEquals(
				IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2019-01-05"), REGION_ID, 20)
						.blockingGet()
						.getAverage());
		assertEquals(
				IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2019-01-05"), REGION_ID, 21)
						.blockingGet()
						.getAverage());
	}

	private void assertYearlyImports() {
		assertEquals(
				NOT_IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2018-12-01"), REGION_ID, 20)
						.blockingGet()
						.getAverage());
		assertNull(marketHistoryDao
				.fetchByPK(LocalDate.parse("2018-12-01"), REGION_ID, 21)
				.blockingGet());

		assertEquals(
				IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2018-12-02"), REGION_ID, 20)
						.blockingGet()
						.getAverage());
		assertEquals(
				IMPORTED,
				marketHistoryDao
						.fetchByPK(LocalDate.parse("2018-12-02"), REGION_ID, 21)
						.blockingGet()
						.getAverage());
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				if (path.equals("/data/")) {
					return mockResponse(ResourceUtil.loadContextual(ImportMarketHistoryTest.class, "/data.html"));
				}
				if (path.equals("/data/market-history/totals.json")) {
					return mockResponse(objectMapper.writeValueAsBytes(Map.of(
							"2019-01-03", 2,
							"2019-01-04", 2,
							"2019-01-05", 2)));
				}
				if (path.equals("/data/market-history/market-history-2018.tar.bz2")) {
					return mockHistoricalYear();
				}
				if (path.equals("/data/market-history/2019/market-history-2019-01-01.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2019-01-01"));
				}
				if (path.equals("/data/market-history/2019/market-history-2019-01-02.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2019-01-02"));
				}
				if (path.equals("/data/market-history/2019/market-history-2019-01-03.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2019-01-03"));
				}
				if (path.equals("/data/market-history/2019/market-history-2019-01-04.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2019-01-04"));
				}
				if (path.equals("/data/market-history/2019/market-history-2019-01-05.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2019-01-05"));
				}
				log.error(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(500);
			} catch (Exception e) {
				fail("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	@NotNull
	private MockResponse mockResponse(byte[] body) {
		return new MockResponse().setResponseCode(200).setBody(new Buffer().write(body));
	}

	@NotNull
	@SneakyThrows
	private MockResponse mockResponse(InputStream in) {
		try (in) {
			var body = IOUtils.toByteArray(in);
			return mockResponse(body);
		}
	}

	@SneakyThrows
	private MockResponse mockHistoricalDate(LocalDate date) {
		var file = ResourceUtil.loadContextual(ImportMarketHistoryTest.class, "/daily-" + date.toString() + ".csv");
		var compressed = new ByteArrayOutputStream();
		try (var out = new BZip2CompressorOutputStream(compressed)) {
			IOUtils.copy(file, out);
		}
		return mockResponse(new ByteArrayInputStream(compressed.toByteArray()));
	}

	@SneakyThrows
	private MockResponse mockHistoricalYear() {
		var file1 = IOUtils.toByteArray(
				ResourceUtil.loadContextual(ImportMarketHistoryTest.class, "/yearly-2018-12-01.csv"));
		var file2 = IOUtils.toByteArray(
				ResourceUtil.loadContextual(ImportMarketHistoryTest.class, "/yearly-2018-12-02.csv"));
		var bytes = testDataUtil.createBz2Tar(Map.of(
				"2018-12-01.csv", file1,
				"2018-12-02.csv", file2));
		return mockResponse(new ByteArrayInputStream(bytes));
	}
}
