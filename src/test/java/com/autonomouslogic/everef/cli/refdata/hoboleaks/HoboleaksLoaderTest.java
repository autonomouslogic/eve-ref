package com.autonomouslogic.everef.cli.refdata.hoboleaks;

import com.autonomouslogic.everef.cli.refdata.RefDataAsserter;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.model.refdata.RefDataConfig;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
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
	MVStoreUtil mvStoreUtil;

	@Inject
	MockScrapeBuilder mockScrapeBuilder;

	@Inject
	RefDataAsserter refDataAsserter;

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
		hoboleaksLoader.load(mockScrapeBuilder.createTestHoboleaksSde()).blockingAwait();
		refDataAsserter.assertTestOutput(
				HoboleaksLoaderTest.class, RefDataConfig::getHoboleaks, storeHandler::getHoboleaksStore);
	}
}
