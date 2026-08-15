package com.autonomouslogic.everef.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.everef.refdata.InventoryType;
import java.nio.file.Files;
import lombok.SneakyThrows;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

public class LoadedRefDataTest {

	private static final JsonMapper OBJECT_MAPPER = new JsonMapper();

	@Test
	@SneakyThrows
	void getTypeShouldReturnNullRatherThanThrowAfterClose() {
		var tempFile = Files.createTempFile("test-loaded-ref-data", ".db");
		tempFile.toFile().deleteOnExit();

		var writeStore = new MVStore.Builder().fileName(tempFile.toString()).open();
		var writeRefData = new LoadedRefData(writeStore);
		writeRefData.jsonMapper = OBJECT_MAPPER;
		for (long i = 0; i < 2000; i++) {
			writeRefData.putType(i, InventoryType.builder().typeId(i).build());
		}
		writeStore.close();

		var readStore = new MVStore.Builder().fileName(tempFile.toString()).open();
		var readRefData = new LoadedRefData(readStore);
		readRefData.jsonMapper = OBJECT_MAPPER;

		readRefData.close();

		assertDoesNotThrow(() -> assertNull(readRefData.getType(1000L)));
	}
}
