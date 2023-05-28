package com.autonomouslogic.everef.cli.refdata.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.model.refdata.RefDataConfig;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

@Log4j2
public class EsiLoaderTest {
	@Inject
	EsiLoader esiLoader;

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
		var mvstore = mvStoreUtil.createTempStore(EsiLoaderTest.class.getSimpleName());
		storeHandler = new StoreHandler(mvStoreUtil, mvstore);
		esiLoader.setStoreHandler(storeHandler);
	}

	@Test
	@SneakyThrows
	void testLoadEsi() {
		esiLoader.load(testDataUtil.createTestEsiDump()).blockingAwait();
		for (RefDataConfig config : refDataUtil.loadReferenceDataConfig()) {
			var testConfig = config.getTest();
			var store = storeHandler.getEsiStore(config.getId());
			for (var id : testConfig.getIds()) {
				var expectedType = objectMapper.readTree(ResourceUtil.loadContextual(
						EsiLoaderTest.class, "/" + testConfig.getFilePrefix() + "-" + id + ".json"));
				testDataUtil.assertJsonStrictEquals(expectedType, store.get(id));
			}
		}
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/cli/refdata/esi/EsiLoaderTest/filenames.csv")
	void shouldParseFileNames(String filename, String fileType, String language) {
		assertEquals(fileType, esiLoader.getFileType(filename));
		assertEquals(language, esiLoader.getLanguage(filename));
	}
}
