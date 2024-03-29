package com.autonomouslogic.everef.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.functions.Consumer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.io.IOUtils;

@Singleton
@Log4j2
public class TestDataUtil {
	public static final int TEST_PORT = 30150;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	RefDataUtil refDataUtil;

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
	public Map<String, byte[]> readFilesFromXzTar(byte[] bytes) {
		Map<String, byte[]> result = new LinkedHashMap<>();
		try (var tar = new TarArchiveInputStream(new XZCompressorInputStream(new ByteArrayInputStream(bytes)))) {
			var entry = tar.getNextTarEntry();
			while (entry != null) {
				if (entry.isDirectory()) {
					continue;
				}
				var file = entry.getName();
				var data = IOUtils.toByteArray(tar);
				result.put(file, data);
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

	@SneakyThrows
	public byte[] createXzTar(Map<String, byte[]> files) {
		var tar = createTar(files);
		var compressed = new ByteArrayOutputStream();
		try (var out = new XZCompressorOutputStream(compressed)) {
			IOUtils.write(tar, out);
		}
		return compressed.toByteArray();
	}

	@SneakyThrows
	public byte[] createBz2Tar(Map<String, byte[]> files) {
		var tar = createTar(files);
		var compressed = new ByteArrayOutputStream();
		try (var out = new BZip2CompressorOutputStream(compressed)) {
			IOUtils.write(tar, out);
		}
		return compressed.toByteArray();
	}

	@SneakyThrows
	public byte[] createTar(Map<String, byte[]> files) {
		var compressed = new ByteArrayOutputStream();
		try (var out = new TarArchiveOutputStream(compressed)) {
			try {
				for (var fileEntry : files.entrySet()) {
					var tarEntry = new TarArchiveEntry(fileEntry.getKey());
					tarEntry.setSize(fileEntry.getValue().length);
					out.putArchiveEntry(tarEntry);
					out.write(fileEntry.getValue());
					out.closeArchiveEntry();
				}
			} catch (Exception e) {
				log.error("Error writing", e);
				throw e;
			}
		}
		return compressed.toByteArray();
	}

	@SneakyThrows
	public MockResponse mockResponse(InputStream in) {
		try (in) {
			return new MockResponse().setResponseCode(200).setBody(new Buffer().write(IOUtils.toByteArray(in)));
		}
	}

	@SneakyThrows
	public MockResponse mockResponse(ByteArrayOutputStream out) {
		return mockResponse(out.toByteArray());
	}

	@SneakyThrows
	public MockResponse mockResponse(File in) {
		return mockResponse(new FileInputStream(in));
	}

	@SneakyThrows
	public MockResponse mockResponse(byte[] in) {
		return mockResponse(new ByteArrayInputStream(in));
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
	public void assertJsonStrictEquals(JsonNode expected, JsonNode actual) {
		var prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();
		assertEquals(prettyWriter.writeValueAsString(expected), prettyWriter.writeValueAsString(actual));
	}

	@SneakyThrows
	public List<RecordedRequest> takeAllRequests(MockWebServer server) {
		var list = new ArrayList<RecordedRequest>();
		RecordedRequest r;
		while ((r = server.takeRequest(1, TimeUnit.MILLISECONDS)) != null) {
			list.add(r);
		}
		return list;
	}
}
