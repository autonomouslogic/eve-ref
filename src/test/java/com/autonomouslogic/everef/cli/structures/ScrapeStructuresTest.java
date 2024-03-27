package com.autonomouslogic.everef.cli.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@SetEnvironmentVariable(key = "SCRAPE_CHARACTER_OWNER_HASH", value = "scrape-owner-hash")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "EVE_REF_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeStructuresTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@Log4j2
@ExtendWith(MockitoExtension.class)
public class ScrapeStructuresTest {
	static final String BUCKET_NAME = "data-bucket";
	static final ZonedDateTime TIME = ZonedDateTime.parse("2021-01-01T00:00:00Z");

	@Inject
	ScrapeStructures scrapeStructures;

	@Mock
	LocationPopulator locationPopulator;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	MockWebServer server;

	JsonNode previousScrape;
	Map<Long, Map<String, String>> publicStructures;

	@Inject
	ScrapeStructuresTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);
		when(locationPopulator.populate(any())).thenReturn(Completable.complete()); // @todo

		previousScrape = null;
		publicStructures = new HashMap<>();

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	void shouldScrapePublicStructures() {
		publicStructures.put(1000000000001L, Map.of("name", "Test Structure 1"));
		scrapeStructures.run().blockingAwait();
		runAndVerifyScrape("/single-public-structure.json");
	}

	@Test
	void shouldUpdatePublicStructures() {
		loadPreviousScrape("/single-public-structure.json");
		publicStructures.put(1000000000001L, Map.of("name", "Test Structure 1 Updated"));
		scrapeStructures.run().blockingAwait();
		runAndVerifyScrape("/single-public-structure-updated.json");
	}

	@SneakyThrows
	private void loadPreviousScrape(String file) {
		previousScrape = objectMapper.readTree(ResourceUtil.loadContextual(getClass(), file));
	}

	@SneakyThrows
	private void runAndVerifyScrape(@NonNull String expectedFile) {
		var archiveFile = "base/structures/structures-latest.v2.json";
		var content = mockS3Adapter
				.getTestObject(BUCKET_NAME, archiveFile, dataClient)
				.orElseThrow();
		var expected = objectMapper.readTree(ResourceUtil.loadContextual(getClass(), expectedFile));
		var supplied = objectMapper.readTree(content);
		assertEquals(expected, supplied);
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var segments = request.getRequestUrl().pathSegments();
				var lastModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(TIME);

				if (path.equals("/structures/structures-latest.v2.json")) {
					if (previousScrape == null) {
						return new MockResponse().setResponseCode(404);
					} else {
						return new MockResponse().setBody(objectMapper.writeValueAsString(previousScrape));
					}
				}
				if (path.equals("/universe/structures/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(publicStructures.keySet()))
							.addHeader("Last-Modified", lastModified);
				}
				if (path.startsWith("/universe/structures/")) {
					var id = Long.parseLong(segments.get(2));
					var structure = publicStructures.get(id);
					if (structure == null) {
						return new MockResponse().setResponseCode(401);
					} else {
						return new MockResponse()
								.setBody(objectMapper.writeValueAsString(structure))
								.addHeader("Last-Modified", lastModified);
					}
				}

				log.error(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
