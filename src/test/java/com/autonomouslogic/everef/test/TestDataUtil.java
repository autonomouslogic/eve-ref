package com.autonomouslogic.everef.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.refdata.sde.SdeLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.functions.Consumer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.io.IOUtils;

@Singleton
public class TestDataUtil {
	public static final int TEST_PORT = 30150;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	protected TestDataUtil() {}

	/**
	 * Reads a bz2 file and decodes the CSV data into a list of maps.
	 * @param bytes
	 * @return
	 */
	@SneakyThrows
	public List<Map<String, String>> readMapsFromBz2Csv(byte[] bytes) {
		// @todo maybe replace with JsonNodeCsvReader
		try (var in = new BZip2CompressorInputStream(new ByteArrayInputStream(bytes))) {
			return readMapsFromCsv(in);
		}
	}

	/**
	 * Reads a CSV file and decodes the data into a list of maps.
	 * @param in
	 * @return
	 */
	@SneakyThrows
	public List<Map<String, String>> readMapsFromCsv(InputStream in) {
		// @todo maybe replace with JsonNodeCsvReader
		var csv = IOUtils.toString(new InputStreamReader(in));
		if (csv.startsWith("{") || csv.trim().isEmpty()) {
			return List.of();
		}
		var records =
				CSVFormat.RFC4180
						.builder()
						.setHeader()
						.setSkipHeaderRecord(true)
						.build()
						.parse(new StringReader(csv))
						.stream()
						.map(r -> r.toMap())
						.collect(Collectors.toList());
		return records;
	}

	/**
	 * Reads a tar.bz2 file and decodes the CSV data for each file into a list of maps.
	 * @param bytes
	 * @return
	 */
	@SneakyThrows
	public Map<String, List<Map<String, String>>> readFileMapsFromBz2TarCsv(byte[] bytes) {
		// @todo maybe replace with JsonNodeCsvReader
		Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
		try (var tar = new TarArchiveInputStream(new BZip2CompressorInputStream(new ByteArrayInputStream(bytes)))) {
			var entry = tar.getNextTarEntry();
			while (entry != null) {
				if (entry.isDirectory()) {
					continue;
				}
				var file = entry.getName();
				var maps = readMapsFromCsv(tar);
				result.put(file, maps);
				entry = tar.getNextTarEntry();
			}
		}
		return result;
	}

	@SneakyThrows
	public List<Map<String, String>> readMapsFromJson(InputStream in) {
		// @todo maybe replace with JsonNodeCsvReader
		var typeFactory = objectMapper.getTypeFactory();
		var map = typeFactory.constructMapType(LinkedHashMap.class, String.class, String.class);
		var list = typeFactory.constructCollectionType(List.class, map);
		return objectMapper.readValue(in, list);
	}

	public void assertRequest(RecordedRequest request, String path) {
		assertRequest(request, "GET", path, null);
	}

	public void assertRequest(RecordedRequest request, String path, String expected) {
		assertRequest(request, "POST", path, body -> assertEquals(expected, body));
	}

	@SneakyThrows
	public void assertRequest(RecordedRequest request, String method, String path, Consumer<String> bodyTester) {
		assertEquals(path, request.getPath());
		assertEquals(method, request.getMethod());
		if (bodyTester == null) {
			assertEquals(0, request.getBodySize());
		} else {
			bodyTester.accept(request.getBody().readUtf8());
		}
	}

	public void assertUserAgent(RecordedRequest request) {
		assertEquals("everef.net", request.getHeader("User-Agent"));
	}

	@SneakyThrows
	public void assertNoMoreRequests(MockWebServer server) {
		assertNull(server.takeRequest(1, TimeUnit.MILLISECONDS));
	}

	@SneakyThrows
	public File createZipFile(Map<String, byte[]> entries) {
		var file = File.createTempFile(TestDataUtil.class.getSimpleName() + "-", ".zip");
		try (var out = new ZipOutputStream(new FileOutputStream(file))) {
			for (var entry : entries.entrySet()) {
				out.putNextEntry(new ZipEntry(entry.getKey()));
				out.write(entry.getValue());
				out.closeEntry();
			}
		}
		return file;
	}

	@SneakyThrows
	public File createTarXzFile(Map<String, byte[]> entries) {
		var file = File.createTempFile(TestDataUtil.class.getSimpleName() + "-", ".tar.xz");
		try (var out = new TarArchiveOutputStream(new XZCompressorOutputStream(new FileOutputStream(file)))) {
			for (var entry : entries.entrySet()) {
				var bytes = entry.getValue();
				var archiveEntry = new TarArchiveEntry(entry.getKey());
				archiveEntry.setSize(bytes.length);
				out.putArchiveEntry(archiveEntry);
				out.write(bytes);
				out.closeArchiveEntry();
			}
		}
		return file;
	}

	@SneakyThrows
	public void assertJsonStrictEquals(JsonNode expected, JsonNode actual) {
		var prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();
		assertEquals(prettyWriter.writeValueAsString(expected), prettyWriter.writeValueAsString(actual));
	}

	@SneakyThrows
	public File createTestSde() {
		return createZipFile(Map.ofEntries(createEntry("/refdata/", SdeLoader.SDE_TYPES_PATH)));
	}

	@SneakyThrows
	public File createTestEsiDump() {
		return createTarXzFile(Map.ofEntries(
				createEntry("/refdata/esi", "data/tranquility/universe/types.en-us.yaml"),
				createEntry("/refdata/esi", "data/tranquility/universe/types.fr.yaml")));
	}

	@SneakyThrows
	private Map.Entry<String, byte[]> createEntry(String base, String path) {
		return Map.entry(path, IOUtils.toByteArray(ResourceUtil.loadResource(base + "/" + path)));
	}
}
