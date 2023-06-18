package com.autonomouslogic.everef.cli.refdata.sde;

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
public class SdeLoaderTest {
	@Inject
	SdeLoader sdeLoader;

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
		var mvstore = mvStoreUtil.createTempStore(SdeLoaderTest.class.getSimpleName());
		storeHandler = new StoreHandler(mvStoreUtil, mvstore);
		sdeLoader.setStoreHandler(storeHandler);
	}

	@Test
	@SneakyThrows
	void testLoadSde() {
		sdeLoader.load(mockScrapeBuilder.createTestSde()).blockingAwait();
		refDataAsserter.assertTestOutput(SdeLoaderTest.class, RefDataConfig::getSde, storeHandler::getSdeStore);
	}
}
