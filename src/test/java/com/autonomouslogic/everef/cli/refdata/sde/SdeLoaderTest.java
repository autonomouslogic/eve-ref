package com.autonomouslogic.everef.cli.refdata.sde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
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
		sdeLoader.load(testDataUtil.createTestSde()).blockingAwait();
		var typeStore = storeHandler.getSdeStore("types");
		var dogmaAttributesStore = storeHandler.getSdeStore("dogmaAttributes");

		assertEquals(1, typeStore.size());
		var expectedType = objectMapper.readTree(ResourceUtil.loadContextual(SdeLoaderTest.class, "/type-645.json"));
		testDataUtil.assertJsonStrictEquals(expectedType, typeStore.get(645L));

		assertEquals(1, dogmaAttributesStore.size());
		var expectedDogmaAttribute =
				objectMapper.readTree(ResourceUtil.loadContextual(SdeLoaderTest.class, "/dogma-attribute-9.json"));
		testDataUtil.assertJsonStrictEquals(expectedDogmaAttribute, dogmaAttributesStore.get(9L));
	}
}
