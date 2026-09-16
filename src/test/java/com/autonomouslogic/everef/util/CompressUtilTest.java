package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class CompressUtilTest {
	@Test
	@SneakyThrows
	void shouldReadAllEntriesFromArchive() {
		byte[] zip = buildZip(List.of(Pair.of("first.txt", "hello"), Pair.of("second.txt", "world")));

		List<Pair<ArchiveEntry, byte[]>> entries;
		try (var stream = CompressUtil.loadArchive(new ZipArchiveInputStream(new ByteArrayInputStream(zip)))) {
			entries = stream.collect(Collectors.toList());
		}

		assertEquals(2, entries.size());
		assertEquals("first.txt", entries.get(0).getLeft().getName());
		assertEquals("hello", new String(entries.get(0).getRight(), StandardCharsets.UTF_8));
		assertEquals("second.txt", entries.get(1).getLeft().getName());
		assertEquals("world", new String(entries.get(1).getRight(), StandardCharsets.UTF_8));
	}

	@Test
	@SneakyThrows
	void shouldCloseUnderlyingStreamWhenExhausted() {
		byte[] zip = buildZip(List.of(Pair.of("a.txt", "content")));
		var real = new ZipArchiveInputStream(new ByteArrayInputStream(zip));
		var spied = spy(real);

		try (var stream = CompressUtil.loadArchive(spied)) {
			stream.collect(Collectors.toList());
		}

		verify(spied).close();
	}

	@Test
	@SneakyThrows
	void shouldCloseUnderlyingStreamWhenErrorOccursDuringIteration() {
		byte[] zip = buildZip(List.of(Pair.of("a.txt", "content"), Pair.of("b.txt", "content2")));
		var real = new ZipArchiveInputStream(new ByteArrayInputStream(zip));
		var spied = spy(real);
		doThrow(new IOException("simulated read error")).when(spied).getNextEntry();

		try (var stream = CompressUtil.loadArchive(spied)) {
			assertThrows(Exception.class, () -> stream.collect(Collectors.toList()));
		}

		verify(spied).close();
	}

	@SneakyThrows
	private static byte[] buildZip(List<Pair<String, String>> entries) {
		var out = new ByteArrayOutputStream();
		try (var zip = new ZipArchiveOutputStream(out)) {
			for (var entry : entries) {
				byte[] bytes = entry.getRight().getBytes(StandardCharsets.UTF_8);
				var zipEntry = new ZipArchiveEntry(entry.getLeft());
				zipEntry.setSize(bytes.length);
				zip.putArchiveEntry(zipEntry);
				zip.write(bytes);
				zip.closeArchiveEntry();
			}
		}
		return out.toByteArray();
	}
}
