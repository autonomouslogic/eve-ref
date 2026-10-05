package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.model.ReferenceEntry;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

public class RefDataUtilTest {
	@Inject
	RefDataUtil refDataUtil;

	@Inject
	JsonMapper jsonMapper;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	void shouldLoadReferenceDataConfig() {
		var config = refDataUtil.loadReferenceDataConfig().stream()
				.filter(c -> c.getId().equals("types"))
				.findFirst()
				.orElseThrow();
		assertEquals("types", config.getId());
		assertEquals("InventoryType", config.getModel());
	}

	@Test
	void test() {
		System.out.println(ZonedDateTime.parse("2024-12-14T20:00:00+09"));
	}

	@Test
	@SneakyThrows
	void shouldParseReferenceDataArchive() {
		var file = buildArchive(Map.of(
				"meta.json",
				"{\"buildTime\":\"2024-01-01T00:00:00Z\"}",
				"types.json",
				"{\"1\":{\"name\":\"a\"},\"2\":{\"name\":\"b\"}}",
				"categories.json",
				"{\"5\":{\"name\":\"c\"}}",
				"ignored.txt",
				"not json"));

		List<ReferenceEntry> entries =
				refDataUtil.parseReferenceDataArchive(file).toList().blockingGet();

		var meta = entries.stream()
				.filter(e -> e.getType().equals("meta"))
				.findFirst()
				.orElseThrow();
		assertNull(meta.getId());
		assertEquals("{\"buildTime\":\"2024-01-01T00:00:00Z\"}", new String(meta.getContent(), StandardCharsets.UTF_8));

		var types = entries.stream().filter(e -> e.getType().equals("types")).collect(Collectors.toList());
		assertEquals(2, types.size());
		assertTrue(types.stream()
				.anyMatch(e -> e.getId().equals(1L)
						&& jsonMapper
								.readTree(e.getContent())
								.get("name")
								.asString()
								.equals("a")));
		assertTrue(types.stream()
				.anyMatch(e -> e.getId().equals(2L)
						&& jsonMapper
								.readTree(e.getContent())
								.get("name")
								.asString()
								.equals("b")));

		var categories =
				entries.stream().filter(e -> e.getType().equals("categories")).collect(Collectors.toList());
		assertEquals(1, categories.size());
		assertEquals(5L, categories.get(0).getId());

		assertEquals(4, entries.size());
		assertFalse(file.exists());
	}

	@SneakyThrows
	private static File buildArchive(Map<String, String> files) {
		var file = File.createTempFile("refdata-test", ".zip");
		var out = new ByteArrayOutputStream();
		try (var zip = new ZipArchiveOutputStream(out)) {
			for (var fileEntry : files.entrySet()) {
				var bytes = fileEntry.getValue().getBytes(StandardCharsets.UTF_8);
				var zipEntry = new ZipArchiveEntry(fileEntry.getKey());
				zipEntry.setSize(bytes.length);
				zip.putArchiveEntry(zipEntry);
				zip.write(bytes);
				zip.closeArchiveEntry();
			}
		}
		java.nio.file.Files.write(file.toPath(), out.toByteArray());
		return file;
	}
}
