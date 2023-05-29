package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
	TestDataUtil testDataUtil;

	@Inject
	ObjectMapper objectMapper;

	MockWebServer server;

	@Inject
	protected PublishRefData publishRefData;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.enqueue(testDataUtil.mockResponse(testDataUtil.createTestRefdata()));
		server.start(TEST_PORT);

		mockS3Adapter.putTestObject(BUCKET_NAME, "index.html", "test", s3);
		mockS3Adapter.putTestObject(BUCKET_NAME, "extra", "test", s3); // Not deleted, outside base path.
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/index.html", "test", s3);
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/types", "[645]", s3); // Not uploaded, content matches.
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/types/645", "test", s3);
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/types/999999999", "test", s3);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	void shouldBuildRefData() {
		publishRefData.run().blockingAwait();

		var putKeys = mockS3Adapter.getAllPutKeys(BUCKET_NAME, s3);
		// `/types` isn't uploaded because it already matches.
		assertEquals(
				Set.of("base/meta", "base/types/645", "base/dogma_attributes", "base/dogma_attributes/9"),
				new HashSet<>(putKeys));

		assertTypes();
		assertDogmaAttributes();

		var deleteKeys = mockS3Adapter.getAllDeleteKeys(BUCKET_NAME, s3);
		assertEquals(List.of("base/types/999999999"), deleteKeys);
	}

	@SneakyThrows
	private void assertTypes() {
		assertType(645);

		var expectedIndex = objectMapper.createArrayNode().add(645);
		var actualIndex = objectMapper.readTree(
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/types", s3).orElseThrow());
		assertEquals(expectedIndex.toString(), actualIndex.toString());
	}

	@SneakyThrows
	private void assertType(long id) {
		var expected = testDataUtil.loadJsonResource("/refdata/refdata/type-" + id + ".json");
		var actual = objectMapper.readTree(
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/types/" + id, s3).orElseThrow());
		assertEquals(expected, actual);
	}

	@SneakyThrows
	private void assertDogmaAttributes() {
		assertDogmaAttribute(9);

		var expectedIndex = objectMapper.createArrayNode().add(9);
		var actualIndex = objectMapper.readTree(mockS3Adapter
				.getTestObject(BUCKET_NAME, "base/dogma_attributes", s3)
				.orElseThrow());
		assertEquals(expectedIndex.toString(), actualIndex.toString());
	}

	@SneakyThrows
	private void assertDogmaAttribute(long id) {
		var expected = testDataUtil.loadJsonResource("/refdata/refdata/dogma-attribute-" + id + ".json");
		var actual = objectMapper.readTree(mockS3Adapter
				.getTestObject(BUCKET_NAME, "base/dogma_attributes/" + id, s3)
				.orElseThrow());
		assertEquals(expected, actual);
	}
}
