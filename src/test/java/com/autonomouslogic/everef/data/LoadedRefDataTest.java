package com.autonomouslogic.everef.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import lombok.SneakyThrows;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.Test;

public class LoadedRefDataTest {

	private static final ObjectMapper OBJECT_MAPPER =
			new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	@SneakyThrows
	void getTypeShouldReturnNullRatherThanThrowAfterClose() {
		var tempFile = Files.createTempFile("test-loaded-ref-data", ".db");
		tempFile.toFile().deleteOnExit();

		var writeStore = new MVStore.Builder().fileName(tempFile.toString()).open();
		var writeRefData = new LoadedRefData(writeStore);
		writeRefData.objectMapper = OBJECT_MAPPER;
		for (long i = 0; i < 2000; i++) {
			writeRefData.putType(i, InventoryType.builder().typeId(i).build());
		}
		writeStore.close();

		var readStore = new MVStore.Builder().fileName(tempFile.toString()).open();
		var readRefData = new LoadedRefData(readStore);
		readRefData.objectMapper = OBJECT_MAPPER;

		readRefData.close();

		assertDoesNotThrow(() -> assertNull(readRefData.getType(1000L)));
	}
}
