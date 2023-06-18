package com.autonomouslogic.everef.cli.refdata.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

@Log4j2
public class EsiLoaderTest {
	@Inject
	EsiLoader esiLoader;

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
		var mvstore = mvStoreUtil.createTempStore(EsiLoaderTest.class.getSimpleName());
		storeHandler = new StoreHandler(mvStoreUtil, mvstore);
		esiLoader.setStoreHandler(storeHandler);
	}

	@Test
	@SneakyThrows
	void testLoadEsi() {
		esiLoader.load(mockScrapeBuilder.createTestEsiDump()).blockingAwait();
		refDataAsserter.assertTestOutput(EsiLoaderTest.class, RefDataConfig::getEsi, storeHandler::getEsiStore);
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/cli/refdata/esi/EsiLoaderTest/filenames.csv")
	void shouldParseFileNames(String filename, String fileType, String language) {
		assertEquals(fileType, esiLoader.getFileType(filename));
		assertEquals(language, esiLoader.getLanguage(filename));
	}
}
