package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.refdata.ObjectMerger;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class SdeLoaderTest {
	@Inject
	SdeLoader sdeLoader;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	MVStoreUtil mvStoreUtil;

	@Inject
	RefDataUtil refDataUtil;

	@Inject
	MockScrapeBuilder mockScrapeBuilder;

	@Inject
	ObjectMerger objectMerger;

	StoreHandler storeHandler;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		var mvstore = mvStoreUtil.createTempStore(SdeLoaderTest.class.getSimpleName());
		storeHandler = new StoreHandler(mvStoreUtil, mvstore);
		sdeLoader.setStoreHandler(storeHandler);
	}

	@Test
	@SneakyThrows
	void testLoadSde() {
		sdeLoader.load(mockScrapeBuilder.createTestSde()).blockingAwait();


		Map<String, Map<Long, JsonNode>> actualFileValues = new LinkedHashMap<>();
		for (var config : refDataUtil.loadReferenceDataConfig()) {
			if (config.getSde() == null) {
				continue;
			}
			var testConfig = config.getTest();
			var actualValues = actualFileValues.computeIfAbsent(testConfig.getFilePrefix(), ignore -> new LinkedHashMap<>());
			var store = storeHandler.getSdeStore(config.getId());
			for (var id : testConfig.getIds()) {
				var existing = actualValues.get(id);
				var json = store.get(id);
				var actual = existing == null ? json : objectMerger.merge(existing, json);
				actualValues.put(id, actual);
			}
		}
		for (Map.Entry<String, Map<Long, JsonNode>> fileEntry : actualFileValues.entrySet()) {
			var filePrefix = fileEntry.getKey();
			var actualValues = fileEntry.getValue();
			for (Map.Entry<Long, JsonNode> valueEntry : actualValues.entrySet()) {
				var id = valueEntry.getKey();
				var actual = valueEntry.getValue();
				var expected = objectMapper.readTree(ResourceUtil.loadContextual(
					SdeLoaderTest.class, "/" + filePrefix + "-" + id + ".json"));
				testDataUtil.assertJsonStrictEquals(expected, actual);
			}
		}
	}
}
