package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
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
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeMilitaryCampaignsTest.BUCKET_NAME + "/base/")
public class ScrapeMilitaryCampaignsTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	ScrapeMilitaryCampaigns scrapeMilitaryCampaigns;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	DataIndexHelper dataIndexHelper;

	MockWebServer server;

	List<ObjectNode> campaignsList;
	Map<String, ArrayNode> objectivesByCampaign;

	@Inject
	protected ScrapeMilitaryCampaignsTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		campaignsList = new ArrayList<>();
		objectivesByCampaign = new HashMap<>();
		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		try {
			assertNull(server.takeRequest(1, TimeUnit.MILLISECONDS));
		} finally {
			server.close();
		}
	}

	@Test
	@SneakyThrows
	void shouldFetchEmptyCampaignList() {
		scrapeMilitaryCampaigns.run();

		var req = server.takeRequest();
		assertEquals("/military-campaigns", req.getRequestUrl().encodedPath());

		var result = readLatest();
		assertTrue(result.isObject());
		assertEquals(0, result.size());
	}

	@Test
	@SneakyThrows
	void shouldFetchCampaignListAndObjectives() {
		createCampaign("campaign-id-1", "Active", 10);
		addObjective("campaign-id-1", "objective-id-1a", "Active", 100);
		addObjective("campaign-id-1", "objective-id-1b", "Active", 200);
		createCampaign("campaign-id-2", "Completed", 100);
		addObjective("campaign-id-2", "objective-id-2a", "Completed", 999);

		scrapeMilitaryCampaigns.run();

		// Verify requests: campaigns list + one objectives request per campaign (order may vary)
		var campaignsReq = server.takeRequest();
		assertEquals("/military-campaigns", campaignsReq.getRequestUrl().encodedPath());
		var objPaths = new java.util.HashSet<String>();
		objPaths.add(server.takeRequest().getRequestUrl().encodedPath());
		objPaths.add(server.takeRequest().getRequestUrl().encodedPath());
		assertTrue(objPaths.contains("/military-campaigns/campaign-id-1/objectives"));
		assertTrue(objPaths.contains("/military-campaigns/campaign-id-2/objectives"));

		var result = readLatest();
		assertEquals(2, result.size());

		var c1 = result.get("campaign-id-1");
		assertNotNull(c1);
		assertEquals("Active", c1.get("state").asText());
		assertEquals(2, c1.get("objectives").size());
		assertNotNull(c1.get("objectives").get("objective-id-1a"));
		assertNotNull(c1.get("objectives").get("objective-id-1b"));

		var c2 = result.get("campaign-id-2");
		assertNotNull(c2);
		assertEquals("Completed", c2.get("state").asText());
		assertEquals(1, c2.get("objectives").size());
		assertNotNull(c2.get("objectives").get("objective-id-2a"));
	}

	@Test
	@SneakyThrows
	void shouldUploadLatestAsPlainJson() {
		createCampaign("campaign-id-1", "Active", 10);

		scrapeMilitaryCampaigns.run();

		server.takeRequest(); // campaigns list
		server.takeRequest(); // objectives

		var latestBytes = mockS3Adapter
				.getTestObject(BUCKET_NAME, "base/military-campaigns/military-campaigns-latest.json", dataClient)
				.orElseThrow();
		// Must parse as plain JSON without decompression
		var result = objectMapper.readTree(latestBytes);
		assertTrue(result.isObject());
		assertNotNull(result.get("campaign-id-1"));
	}

	@Test
	@SneakyThrows
	void shouldUploadArchiveAsCompressedBzip2() {
		createCampaign("campaign-id-1", "Active", 10);

		var scrapeTime = ZonedDateTime.parse("2026-08-06T10:00:00Z").truncatedTo(ChronoUnit.SECONDS);
		scrapeMilitaryCampaigns.setScrapeTime(scrapeTime);
		scrapeMilitaryCampaigns.run();

		server.takeRequest(); // campaigns list
		server.takeRequest(); // objectives

		var archivePath =
				"base/military-campaigns/history/2026/2026-08-06/military-campaigns-2026-08-06_10-00-00.json.bz2";
		var archiveBytes = mockS3Adapter
				.getTestObject(BUCKET_NAME, archivePath, dataClient)
				.orElseThrow();

		var result = decompress(archiveBytes);
		assertNotNull(result.get("campaign-id-1"));
	}

	@Test
	@SneakyThrows
	void shouldUpdateDataIndex() {
		createCampaign("campaign-id-1", "Active", 10);

		var scrapeTime = ZonedDateTime.parse("2026-08-06T10:00:00Z").truncatedTo(ChronoUnit.SECONDS);
		scrapeMilitaryCampaigns.setScrapeTime(scrapeTime);
		scrapeMilitaryCampaigns.run();

		server.takeRequest(); // campaigns list
		server.takeRequest(); // objectives

		verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder()
								.bucket(BUCKET_NAME)
								.path("base/military-campaigns/military-campaigns-latest.json")
								.build(),
						S3Url.builder()
								.bucket(BUCKET_NAME)
								.path(
										"base/military-campaigns/history/2026/2026-08-06/military-campaigns-2026-08-06_10-00-00.json.bz2")
								.build());
	}

	private void createCampaign(String id, String state, int progress) {
		campaignsList.add(objectMapper
				.createObjectNode()
				.put("id", id)
				.put("state", state)
				.put("progress", progress));
	}

	private void addObjective(String campaignId, String objectiveId, String state, int progress) {
		objectivesByCampaign
				.computeIfAbsent(campaignId, k -> objectMapper.createArrayNode())
				.add(objectMapper
						.createObjectNode()
						.put("id", objectiveId)
						.put("state", state)
						.put("progress", progress));
	}

	@SneakyThrows
	private String buildCampaignsResponse() {
		var campaignsArray = objectMapper.createArrayNode();
		campaignsList.forEach(campaignsArray::add);
		return objectMapper.writeValueAsString(objectMapper.createObjectNode().set("campaigns", campaignsArray));
	}

	@SneakyThrows
	private String buildObjectivesResponse(String campaignId) {
		var objectivesArray = objectivesByCampaign.getOrDefault(campaignId, objectMapper.createArrayNode());
		var response = objectMapper.createObjectNode();
		response.set("cursor", objectMapper.createObjectNode().put("after", "next-cursor-token"));
		response.set("objectives", objectivesArray);
		return objectMapper.writeValueAsString(response);
	}

	private JsonNode readLatest() throws IOException {
		var latestBytes = mockS3Adapter
				.getTestObject(BUCKET_NAME, "base/military-campaigns/military-campaigns-latest.json", dataClient)
				.orElseThrow();
		return objectMapper.readTree(latestBytes);
	}

	private JsonNode decompress(byte[] bytes) throws IOException {
		try (var in = new BZip2CompressorInputStream(new ByteArrayInputStream(bytes))) {
			return objectMapper.readTree(in);
		}
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();

				if (path.equals("/military-campaigns")) {
					return new MockResponse().setBody(buildCampaignsResponse());
				}

				if (path.matches("/military-campaigns/[^/]+/objectives")) {
					var parts = path.split("/");
					var campaignId = parts[2];
					return new MockResponse().setBody(buildObjectivesResponse(campaignId));
				}

				log.error("Unaccounted for URL: {}", path);
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
