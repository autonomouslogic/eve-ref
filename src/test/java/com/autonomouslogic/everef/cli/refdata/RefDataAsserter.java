package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.model.refdata.RefDataConfig;
import com.autonomouslogic.everef.model.refdata.RefTypeConfig;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.h2.mvstore.MVMap;

public class RefDataAsserter {
	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected ObjectMerger objectMerger;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected TestDataUtil testDataUtil;

	@Inject
	protected RefDataAsserter() {}

	@SneakyThrows
	public void assertTestOutput(
			Class<?> testClass,
			Function<RefDataConfig, RefTypeConfig> typeConfigProvider,
			Function<String, MVMap<Long, JsonNode>> storeProvider) {
		Map<String, Map<Long, JsonNode>> actualFileValues = new LinkedHashMap<>();
		for (var config : refDataUtil.loadReferenceDataConfig()) {
			if (typeConfigProvider.apply(config) == null) {
				continue;
			}
			var testConfig = config.getTest();
			var actualValues =
					actualFileValues.computeIfAbsent(testConfig.getFilePrefix(), ignore -> new LinkedHashMap<>());
			var store = storeProvider.apply(config.getId());
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
				var expected = objectMapper.readTree(
						ResourceUtil.loadContextual(testClass, "/" + filePrefix + "-" + id + ".json"));
				testDataUtil.assertJsonStrictEquals(expected, actual);
			}
		}
	}
}
