package com.autonomouslogic.everef.refdata.sde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

	MVMap<Long, JsonNode> typeStore;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		var mvstore = mvStoreUtil.createTempStore(SdeLoaderTest.class.getSimpleName());
		typeStore = mvStoreUtil.openJsonMap(mvstore, "types", Long.class);
		sdeLoader.setTypeStore(typeStore);
	}

	@Test
	@SneakyThrows
	void testLoadSde() {
		sdeLoader.load(testDataUtil.createTestSde()).blockingAwait();
		assertEquals(1, typeStore.size());
		var expectedType = objectMapper.readTree(ResourceUtil.loadContextual(SdeLoaderTest.class, "/type-645.json"));
		testDataUtil.assertJsonStrictEquals(expectedType, typeStore.get(645L));
	}
}
