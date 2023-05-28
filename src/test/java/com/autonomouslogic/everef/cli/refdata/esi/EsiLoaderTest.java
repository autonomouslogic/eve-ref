package com.autonomouslogic.everef.cli.refdata.esi;

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
		var typeStore = storeHandler.getEsiStore("types");
		var dogmaAttributesStore = storeHandler.getEsiStore("dogma-attributes");
		esiLoader.load(testDataUtil.createTestEsiDump()).blockingAwait();

		assertEquals(1, typeStore.size());
		var expectedType = objectMapper.readTree(ResourceUtil.loadContextual(EsiLoaderTest.class, "/type-645.json"));
		testDataUtil.assertJsonStrictEquals(expectedType, typeStore.get(645L));

		assertEquals(1, dogmaAttributesStore.size());
		var expectedDogmaAttribute =
				objectMapper.readTree(ResourceUtil.loadContextual(EsiLoaderTest.class, "/dogma-attribute-9.json"));
		testDataUtil.assertJsonStrictEquals(expectedDogmaAttribute, dogmaAttributesStore.get(9L));
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/cli/refdata/esi/EsiLoaderTest/filenames.csv")
	void shouldParseFileNames(String filename, String fileType, String language) {
		assertEquals(fileType, esiLoader.getFileType(filename));
		assertEquals(language, esiLoader.getLanguage(filename));
	}
}
