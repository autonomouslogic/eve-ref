package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "REFERENCE_DATA_PATH", value = "s3://" + PublishRefDataTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
public class PublishRefDataTest {
	static final String BUCKET_NAME = "ref-data-bucket";

	@Inject
	@Named("refdata")
	S3AsyncClient s3;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	DataUtil dataUtil;

	@Inject
	MockScrapeBuilder mockScrapeBuilder;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	RefDataUtil refDataUtil;

	MockWebServer server;

	@Inject
	protected PublishRefData publishRefData;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.enqueue(testDataUtil.mockResponse(mockScrapeBuilder.createTestRefdata()));
		server.start(TEST_PORT);

		mockS3Adapter.putTestObject(BUCKET_NAME, "index.html", "test", s3);
		mockS3Adapter.putTestObject(BUCKET_NAME, "extra", "test", s3); // Not deleted, outside base path.
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/index.html", "test", s3);
		mockS3Adapter.putTestObject(
				BUCKET_NAME, "base/types", expectedTypeIndex(), s3); // Not uploaded, content matches.
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/types/645", "test", s3);
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/types/999999999", "test", s3);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldBuildRefData() {
		publishRefData.run().blockingAwait();

		var putKeys = mockS3Adapter.getAllPutKeys(BUCKET_NAME, s3);

		var expectedKeys = new HashSet<String>();
		expectedKeys.add("base/meta");
		for (var config : refDataUtil.loadReferenceDataConfig()) {
			var testConfig = config.getTest();
			expectedKeys.add("base/" + config.getOutputFile());

			var expectedIndex = objectMapper.valueToTree(
					testConfig.getIds().stream().sorted().toList());
			var actualIndex = objectMapper.readTree(mockS3Adapter
					.getTestObject(BUCKET_NAME, "base/" + config.getOutputFile(), s3)
					.orElseThrow());
			assertEquals(expectedIndex.toString(), actualIndex.toString());

			for (var id : testConfig.getIds()) {
				expectedKeys.add("base/" + config.getOutputFile() + "/" + id);

				var expectedItem = dataUtil.loadJsonResource(
						String.format("/refdata/refdata/%s-%s.json", testConfig.getFilePrefix(), id));
				var actualItem = objectMapper.readTree(mockS3Adapter
						.getTestObject(BUCKET_NAME, String.format("base/%s/%s", config.getOutputFile(), id), s3)
						.orElseThrow());
				assertEquals(expectedItem, actualItem);
			}
		}

		// `/types` isn't uploaded because it already matches.
		expectedKeys.remove("base/types");
		assertEquals(expectedKeys, new HashSet<>(putKeys));

		var deleteKeys = mockS3Adapter.getAllDeleteKeys(BUCKET_NAME, s3);
		assertEquals(List.of("base/types/999999999"), deleteKeys);
	}

	@SneakyThrows
	private String expectedTypeIndex() {
		var types = refDataUtil.loadReferenceDataConfig().stream()
				.filter(config -> config.getOutputFile().equals("types"))
				.findFirst()
				.orElseThrow();
		var ids = types.getTest().getIds().stream().sorted().toList();
		var json = objectMapper.writeValueAsString(ids);
		return json;
	}
}
