package com.autonomouslogic.everef.cli.refdata.esi;

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

	MVMap<Long, JsonNode> typeStore;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		var mvstore = mvStoreUtil.createTempStore(EsiLoaderTest.class.getSimpleName());
		typeStore = mvStoreUtil.openJsonMap(mvstore, "types", Long.class);
		esiLoader.setTypeStore(typeStore);
	}

	@Test
	@SneakyThrows
	void testLoadEsi() {
		esiLoader.load(testDataUtil.createTestEsiDump()).blockingAwait();
		assertEquals(1, typeStore.size());
		var expectedType = objectMapper.readTree(ResourceUtil.loadContextual(EsiLoaderTest.class, "/type-645.json"));
		testDataUtil.assertJsonStrictEquals(expectedType, typeStore.get(645L));
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/cli/refdata/esi/EsiLoaderTest/filenames.csv")
	void shouldParseFileNames(String filename, String fileType, String language) {
		assertEquals(fileType, esiLoader.getFileType(filename));
		assertEquals(language, esiLoader.getLanguage(filename));
	}
}
