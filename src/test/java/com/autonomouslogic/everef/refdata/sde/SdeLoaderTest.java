package com.autonomouslogic.everef.refdata.sde;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Log4j2
public class SdeLoaderTest {
	@Inject
	SdeLoader sdeLoader;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	TestDataUtil testDataUtil;

	Map<Long, JsonNode> typeStore;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		typeStore = new LinkedHashMap<>();
		sdeLoader.setTypeStore(typeStore);
	}

	@Test
	@SneakyThrows
	void testLoadSde() {
		sdeLoader.load(createTestSde()).blockingAwait();
		assertEquals(1, typeStore.size());

		var expectedType = objectMapper.readTree(ResourceUtil.loadContextual(SdeLoaderTest.class, "/type-645.json"));

		var basePrice = typeStore.get(645L).get("base_price");

		assertEquals(expectedType.toString(), typeStore.get(645L).toString());
	}

	@SneakyThrows
	private File createTestSde() {
		return testDataUtil.createZipFile(Map.ofEntries(
			Map.entry("sde/fsd/typeIDs.yaml", IOUtils.toByteArray(ResourceUtil.loadContextual(SdeLoaderTest.class, "/fsd/typeIDs.yaml")))
		));
	}
}
