package com.autonomouslogic.everef.cli.refdata;

import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.http.MockDataCrawlerModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import java.io.FileInputStream;
import java.time.ZonedDateTime;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + BuildRefDataTest.BUCKET_NAME + "/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
public class BuildRefDataTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	UrlParser urlParser;

	@Inject
	TempFiles tempFiles;

	MockWebServer server;

	@Inject
	protected BuildRefData buildRefData;

	@BeforeEach
	@SneakyThrows
	void before() {
		var dataCrawler = mock(DataCrawler.class);

		DaggerTestComponent.builder()
				.mockDataCrawlerModule(new MockDataCrawlerModule().setDataCrawler(dataCrawler))
				.build()
				.inject(this);

		when(dataCrawler.crawl())
				.thenReturn(Flowable.just(
						urlParser.parse("http://localhost:" + TEST_PORT + "/ccp/sde/sde-20230315-TRANQUILITY.zip")));
		when(dataCrawler.setPrefix(any())).thenReturn(dataCrawler);

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	void shouldBuildRefData() {
		buildRefData
				.setBuildTime(ZonedDateTime.parse("2022-01-05T04:05:06.89Z"))
				.run()
				.blockingAwait();

		// Get saved file.
		var archiveFile = "reference-data/history/2022/reference-data-2022-01-05.tar.xz";
		var latestFile = "reference-data/reference-data-latest.tar.xz";
		var content = mockS3Adapter
				.getTestObject(BUCKET_NAME, archiveFile, dataClient)
				.orElseThrow();
		// Assert the two files are the same.
		mockS3Adapter.assertSameContent(BUCKET_NAME, archiveFile, latestFile, dataClient);

		// Assert records.
		var files = testDataUtil.readFilesFromXzTar(content);
		assertEquals(Set.of("meta.json", "types.json"), files.keySet());
		assertMeta(files.get("meta.json"));
		assertTypes(files.get("types.json"));
	}

	@SneakyThrows
	private void assertTypes(@NonNull byte[] json) {
		var expected = objectMapper.createObjectNode();
		expected.set("645", objectMapper.readTree(ResourceUtil.loadResource("/refdata/refdata/type-645.json")));
		var supplied = objectMapper.readTree(json);
		assertEquals(expected, supplied);

		// Check the encoded JSON contains full numbers. This comes from type 645 Dominix.
		assertTrue(new String(json).contains("\"base_price\":153900000"));
	}

	@SneakyThrows
	private void assertMeta(@NonNull byte[] json) {
		var expected = objectMapper.createObjectNode().put("build_time", "2022-01-05T04:05:06.890Z");
		var supplied = objectMapper.readTree(json);
		assertEquals(expected, supplied);
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				log.info("Path: {}", path);
				switch (path) {
					case "/ccp/sde/sde-20230315-TRANQUILITY.zip":
						var sde = testDataUtil.createTestSde();
						return new MockResponse()
								.setResponseCode(200)
								.setBody(new Buffer().write(IOUtils.toByteArray(new FileInputStream(sde))));
					case "/esi-scrape/eve-ref-esi-scrape-latest.tar.xz":
						var esi = testDataUtil.createTestEsiDump();
						return new MockResponse()
								.setResponseCode(200)
								.setBody(new Buffer().write(IOUtils.toByteArray(new FileInputStream(esi))));
				}
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
