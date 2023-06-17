package com.autonomouslogic.everef.cli.refdata.hoboleaks;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Log4j2
public class HoboleaksLoaderTest {
	@Inject
	HoboleaksLoader hoboleaksLoader;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	MVStoreUtil mvStoreUtil;

	@Inject
	RefDataUtil refDataUtil;

	StoreHandler storeHandler;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		var mvstore = mvStoreUtil.createTempStore(HoboleaksLoaderTest.class.getSimpleName());
		storeHandler = new StoreHandler(mvStoreUtil, mvstore);
		hoboleaksLoader.setStoreHandler(storeHandler);
	}

	@Test
	@SneakyThrows
	void testLoadHoboleaks() {
		hoboleaksLoader.load(testDataUtil.createTestHoboleaksSde()).blockingAwait();
		for (var config : refDataUtil.loadReferenceDataConfig()) {
			if (config.getHoboleaks() == null) {
				continue;
			}
			var testConfig = config.getTest();
			var store = storeHandler.getHoboleaksStore(config.getId());
			for (var id : testConfig.getIds()) {
				var expectedType = objectMapper.readTree(ResourceUtil.loadContextual(
						HoboleaksLoaderTest.class, "/" + testConfig.getFilePrefix() + "-" + id + ".json"));
				testDataUtil.assertJsonStrictEquals(expectedType, store.get(id));
			}
		}
	}
}
