package com.autonomouslogic.everef.cli.markethistory.imports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.db.MarketHistoryDao;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
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
 *     <li>2019-01-01 not present in totals.json</li>
 *     <li>2019-01-02 present in totals.json</li>
 *     <li>2019-01-03 present in totals.json</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/data/")
@SetEnvironmentVariable(key = "IMPORT_MARKET_HISTORY_MIN_DATE", value = "2018-01-01")
public class ImportMarketHistoryTest {
	@Inject
	DbAccess dbAccess;

	@Inject
	ImportMarketHistory importMarketHistory;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	MarketHistoryDao marketHistoryDao;

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

		assertNotNull(marketHistoryDao
				.fetchByPK(LocalDate.parse("2019-01-01"), 10000001, 20)
				.blockingGet());
		assertNotNull(marketHistoryDao
				.fetchByPK(LocalDate.parse("2019-01-02"), 10000001, 20)
				.blockingGet());
		assertNotNull(marketHistoryDao
				.fetchByPK(LocalDate.parse("2019-01-03"), 10000001, 20)
				.blockingGet());
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
							"2019-01-01", 0,
							"2019-01-02", 0,
							"2019-01-03", 0)));
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
		var file = ResourceUtil.loadContextual(ImportMarketHistoryTest.class, "/data-" + date.toString() + ".csv");
		var compressed = new ByteArrayOutputStream();
		try (var out = new BZip2CompressorOutputStream(compressed)) {
			IOUtils.copy(file, out);
		}
		return mockResponse(new ByteArrayInputStream(compressed.toByteArray()));
	}
}
