package com.autonomouslogic.everef.cli.publishrefdata;

import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.model.refdata.RefDataConfig;
import com.autonomouslogic.everef.model.refdata.RefTestConfig;
import com.autonomouslogic.everef.refdata.RefDataMeta;
import com.autonomouslogic.everef.refdata.RefDataMetaFileInfo;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Builder;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.Value;
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
@SetEnvironmentVariable(key = "REFERENCE_DATA_PATH", value = "s3://" + PublishRefDataTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
@SetEnvironmentVariable(key = "REF_DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
public class PublishRefDataTest {
	public static final String BUCKET_NAME = "ref-data-bucket";

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

	RefDataMeta meta;
	File refDataFile;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TEST_PORT);

		mockS3Adapter.putTestObject(BUCKET_NAME, "index.html", "test", s3);
		mockS3Adapter.putTestObject(BUCKET_NAME, "extra", "test", s3); // Not deleted, outside base path.
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/index.html", "test", s3);
		mockS3Adapter.putTestObject(
				BUCKET_NAME, "base/types", expectedTypeIndex(), s3); // Not uploaded, content matches.
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/types/645", "test", s3);
		mockS3Adapter.putTestObject(BUCKET_NAME, "base/types/999999999", "test", s3);

		meta = RefDataMeta.builder()
				.buildTime(Instant.parse("2021-01-01T00:00:00Z"))
				.sde(RefDataMetaFileInfo.builder().sha256("abc").build())
				.esi(RefDataMetaFileInfo.builder().sha256("def").build())
				.hoboleaks(RefDataMetaFileInfo.builder().sha256("ghi").build())
				.build();

		refDataFile = mockScrapeBuilder.createTestRefdata();
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldPublishRefData() {
		publishRefData.run();

		var putKeys = mockS3Adapter.getAllPutKeys(BUCKET_NAME, s3);

		var expectedKeys = new HashSet<String>();
		expectedKeys.add("base/meta");
		expectedKeys.add("base/categories/bundle");
		expectedKeys.add("base/market_groups/root");
		expectedKeys.add("base/market_groups/root/bundle");
		for (var config : refDataUtil.loadReferenceDataConfig()) {
			var testConfig = config.getTest();
			assertIndex(config, testConfig, expectedKeys);
			for (var id : testConfig.getIds()) {
				assertFile(id, config, testConfig, expectedKeys);
				if (config.getId().equals("types")) {
					assertTypeBundle(id, config, testConfig, expectedKeys);
				}
				if (config.getId().equals("categories")) {
					assertCategoryBundle(id, config, testConfig, expectedKeys);
				}
				if (config.getId().equals("groups")) {
					assertGroupBundle(id, config, testConfig, expectedKeys);
				}
				if (config.getId().equals("marketGroups")) {
					assertMarketGroupBundle(id, config, testConfig, expectedKeys);
				}
			}
		}

		// `/types` isn't uploaded because it already matches.
		expectedKeys.remove("base/types");
		assertEquals(
				expectedKeys.stream().sorted().collect(Collectors.joining("\n")),
				putKeys.stream().sorted().collect(Collectors.joining("\n")));

		var deleteKeys = mockS3Adapter.getAllDeleteKeys(BUCKET_NAME, s3);
		assertEquals(List.of("base/types/999999999"), deleteKeys);
	}

	@SneakyThrows
	private void assertIndex(RefDataConfig config, RefTestConfig testConfig, Set<String> expectedKeys) {
		var expectedIndex =
				objectMapper.valueToTree(testConfig.getIds().stream().sorted().toList());
		var path = "base/" + config.getOutputFile();
		expectedKeys.add(path);
		var actualIndex = objectMapper.readTree(mockS3Adapter
				.getTestObject(BUCKET_NAME, path, s3)
				.orElseThrow(() -> new RuntimeException("Missing path: " + path)));
		assertEquals(expectedIndex.toString(), actualIndex.toString());
	}

	@SneakyThrows
	private void assertFile(Long id, RefDataConfig config, RefTestConfig testConfig, Set<String> expectedKeys) {
		var path = String.format("base/%s/%s", config.getOutputFile(), id);
		expectedKeys.add(path);
		var expectedItem =
				dataUtil.loadJsonResource(String.format("/refdata/refdata/%s-%s.json", testConfig.getFilePrefix(), id));
		var actualItem = objectMapper.readTree(mockS3Adapter
				.getTestObject(BUCKET_NAME, path, s3)
				.orElseThrow(() -> new RuntimeException("Missing path: " + path)));
		assertEquals(expectedItem, actualItem);
	}

	@SneakyThrows
	private void assertTypeBundle(long id, RefDataConfig config, RefTestConfig testConfig, Set<String> expectedKeys) {
		var path = String.format("base/%s/%s/bundle", config.getOutputFile(), id);
		expectedKeys.add(path);

		// Dominix
		if (id == 645) {
			assertBundle(
					ExpectedBundleIds.builder()
							.typeIds(List.of(
									645L, 22430L, 3336L, 3327L, 33097L, 3332L, 33093L, 3328L, 999L, 34L, 37152L,
									43733L))
							.attributeIds(List.of(9L, 162L, 182L, 277L, 183L, 278L))
							.skillIds(List.of(3336L, 3327L, 33097L, 3332L, 33093L, 3328L))
							.unitIds(List.of(1L, 133L))
							.iconIds(List.of(67L))
							.marketGroupIds(List.of(81L, 7L, 1376L, 4L))
							.categoryIds(List.of(6L))
							.groupIds(List.of(27L))
							.metaGroupIds(List.of(1L, 2L))
							.build(),
					path);
		}
		// Standup M-Set Basic Large Ship Manufacturing Material Efficiency II
		if (id == 37152) {
			assertBundle(
					ExpectedBundleIds.builder()
							.typeIds(List.of(37152L))
							.attributeIds(List.of(9L, 162L, 182L, 277L, 2355L))
							.skillIds(List.of())
							.unitIds(List.of(1L, 133L))
							.iconIds(List.of(67L))
							.marketGroupIds(List.of())
							.categoryIds(List.of())
							.groupIds(List.of(27L, 513L, 941L))
							.metaGroupIds(List.of())
							.build(),
					path);
		}
		// Standup XL-Set Structure and Component Manufacturing Efficiency I
		if (id == 43704) {
			assertBundle(
					ExpectedBundleIds.builder()
							.typeIds(List.of(43704L))
							.attributeIds(List.of(9L, 162L, 182L, 277L, 2355L))
							.skillIds(List.of())
							.unitIds(List.of(1L, 133L))
							.iconIds(List.of(67L))
							.marketGroupIds(List.of())
							.categoryIds(List.of(66L, 23L))
							.groupIds(List.of(1869L, 332L))
							.metaGroupIds(List.of())
							.build(),
					path);
		}
	}

	@SneakyThrows
	private void assertCategoryBundle(
			long id, RefDataConfig config, RefTestConfig testConfig, Set<String> expectedKeys) {
		var path = String.format("base/%s/%s/bundle", config.getOutputFile(), id);
		expectedKeys.add(path);

		if (id == 6) {
			assertBundle(
					ExpectedBundleIds.builder()
							.categoryIds(List.of(6L))
							.groupIds(List.of(27L, 513L, 898L, 941L))
							.build(),
					path);
		}
	}

	@SneakyThrows
	private void assertGroupBundle(long id, RefDataConfig config, RefTestConfig testConfig, Set<String> expectedKeys) {
		var path = String.format("base/%s/%s/bundle", config.getOutputFile(), id);
		expectedKeys.add(path);

		// Only spot-check one complete one.
		if (id == 27) {
			assertBundle(
					ExpectedBundleIds.builder()
							.groupIds(List.of(27L))
							.typeIds(List.of(645L))
							.attributeIds(List.of(9L, 162L, 182L, 277L))
							.unitIds(List.of(1L, 133L))
							.iconIds(List.of(67L))
							.metaGroupIds(List.of(1L))
							.build(),
					path);
		}
	}

	@SneakyThrows
	private void assertMarketGroupBundle(
			long id, RefDataConfig config, RefTestConfig testConfig, Set<String> expectedKeys) {
		var path = String.format("base/%s/%s/bundle", config.getOutputFile(), id);
		expectedKeys.add(path);

		if (id == 7) {
			assertBundle(
					ExpectedBundleIds.builder()
							.marketGroupIds(List.of(7L, 81L))
							.unitIds(List.of(133L))
							.build(),
					path);
		} else if (id == 81) {
			assertBundle(
					ExpectedBundleIds.builder()
							.marketGroupIds(List.of(81L))
							.typeIds(List.of(645L))
							.attributeIds(List.of(9L, 162L, 182L, 277L))
							.unitIds(List.of(1L, 133L))
							.iconIds(List.of(67L))
							.metaGroupIds(List.of(1L))
							.build(),
					path);
		}
	}

	@SneakyThrows
	private void assertBundle(ExpectedBundleIds expectedBundleIds, String path) {
		var expectedBundle = buildTestBundle(expectedBundleIds);

		var actualItem = objectMapper.readTree(mockS3Adapter
				.getTestObject(BUCKET_NAME, path, s3)
				.orElseThrow(() -> new RuntimeException("Missing path: " + path)));

		var knownBundleFields = Set.of(
				"types",
				"dogma_attributes",
				"skills",
				"units",
				"icons",
				"market_groups",
				"categories",
				"groups",
				"meta_groups");
		var unknownFields = Lists.newArrayList(actualItem.fieldNames()).stream()
				.filter(o -> !knownBundleFields.contains(o))
				.toList();
		assertEquals(List.of(), unknownFields);

		assertExpectedBundleIds(expectedBundleIds.getTypeIds(), actualItem, "types", path);
		assertExpectedBundleIds(expectedBundleIds.getAttributeIds(), actualItem, "dogma_attributes", path);
		assertExpectedBundleIds(expectedBundleIds.getSkillIds(), actualItem, "skills", path);
		assertExpectedBundleIds(expectedBundleIds.getUnitIds(), actualItem, "units", path);
		assertExpectedBundleIds(expectedBundleIds.getIconIds(), actualItem, "icons", path);
		assertExpectedBundleIds(expectedBundleIds.getMarketGroupIds(), actualItem, "market_groups", path);
		assertExpectedBundleIds(expectedBundleIds.getCategoryIds(), actualItem, "categories", path);
		assertExpectedBundleIds(expectedBundleIds.getGroupIds(), actualItem, "groups", path);
		assertExpectedBundleIds(expectedBundleIds.getMetaGroupIds(), actualItem, "meta_groups", path);

		assertEquals(expectedBundle, actualItem);
	}

	private void assertExpectedBundleIds(List<Long> expectedIds, JsonNode bundle, String rootField, String path) {
		var expected = Optional.ofNullable(expectedIds).orElse(List.of()).stream()
				.sorted()
				.toList();
		var supplied = Lists.newArrayList(Optional.ofNullable(bundle.get(rootField))
						.map(JsonNode::fieldNames)
						.orElse(Iterators.forArray()))
				.stream()
				.map(e -> Long.parseLong(e))
				.sorted()
				.toList();
		assertEquals(expected, supplied, rootField + " on " + path);
	}

	private ObjectNode buildTestBundle(ExpectedBundleIds expectedBundleIds) {
		var typeIds = expectedBundleIds.getTypeIds();
		var attributeIds = expectedBundleIds.getAttributeIds();
		var skillIds = expectedBundleIds.getSkillIds();
		var unitIds = expectedBundleIds.getUnitIds();
		var iconIds = expectedBundleIds.getIconIds();
		var marketGroupIds = expectedBundleIds.getMarketGroupIds();
		var categoryIds = expectedBundleIds.getCategoryIds();
		var groupIds = expectedBundleIds.getGroupIds();
		var metaGroupIds = expectedBundleIds.getMetaGroupIds();

		var bundle = objectMapper.createObjectNode();
		if (typeIds != null && !typeIds.isEmpty()) {
			var container = bundle.putObject("types");
			for (long id : typeIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/type-%s.json", id)));
			}
		}
		if (attributeIds != null && !attributeIds.isEmpty()) {
			var container = bundle.putObject("dogma_attributes");
			for (long id : attributeIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/dogma-attribute-%s.json", id)));
			}
		}
		if (skillIds != null && !skillIds.isEmpty()) {
			var container = bundle.putObject("skills");
			for (long id : skillIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/skill-%s.json", id)));
			}
		}
		if (unitIds != null && !unitIds.isEmpty()) {
			var container = bundle.putObject("units");
			for (long id : unitIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/unit-%s.json", id)));
			}
		}
		if (iconIds != null && !iconIds.isEmpty()) {
			var container = bundle.putObject("icons");
			for (long id : iconIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/icon-%s.json", id)));
			}
		}
		if (marketGroupIds != null && !marketGroupIds.isEmpty()) {
			var container = bundle.putObject("market_groups");
			for (long id : marketGroupIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/market-group-%s.json", id)));
			}
		}
		if (categoryIds != null && !categoryIds.isEmpty()) {
			var container = bundle.putObject("categories");
			for (long id : categoryIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/category-%s.json", id)));
			}
		}
		if (groupIds != null && !groupIds.isEmpty()) {
			var container = bundle.putObject("groups");
			for (long id : groupIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/group-%s.json", id)));
			}
		}
		if (metaGroupIds != null && !metaGroupIds.isEmpty()) {
			var container = bundle.putObject("meta_groups");
			for (long id : metaGroupIds) {
				container.set(
						Long.toString(id),
						dataUtil.loadJsonResource(String.format("/refdata/refdata/meta-group-%s.json", id)));
			}
		}
		return bundle;
	}

	@Test
	@SneakyThrows
	void shouldNotPublishRefDataIfThereNoUpdate() {
		refDataFile = mockScrapeBuilder.createTestRefdata(meta);
		publishRefData.run();

		var putKeys = mockS3Adapter.getAllPutKeys(BUCKET_NAME, s3);
		var deleteKeys = mockS3Adapter.getAllDeleteKeys(BUCKET_NAME, s3);
		assertEquals(List.of(), putKeys);
		assertEquals(List.of(), deleteKeys);
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

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				log.info("Path: {}", path);
				switch (path) {
					case "/reference-data/reference-data-latest.tar.xz":
						return new MockResponse()
								.setResponseCode(200)
								.setBody(new Buffer().write(IOUtils.toByteArray(new FileInputStream(refDataFile))));
					case "/meta":
						return new MockResponse()
								.setResponseCode(200)
								.setBody(new Buffer().write(objectMapper.writeValueAsBytes(meta)));
				}
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	@Value
	@Builder
	private static class ExpectedBundleIds {
		@Singular
		List<Long> typeIds;

		@Singular
		List<Long> attributeIds;

		@Singular
		List<Long> skillIds;

		@Singular
		List<Long> unitIds;

		@Singular
		List<Long> iconIds;

		@Singular
		List<Long> marketGroupIds;

		@Singular
		List<Long> categoryIds;

		@Singular
		List<Long> groupIds;

		@Singular
		List<Long> metaGroupIds;
	}
}
