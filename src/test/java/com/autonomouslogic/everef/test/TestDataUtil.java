package com.autonomouslogic.everef.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.csv.CSVFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Singleton
public class TestDataUtil {
	@Inject
	ObjectMapper objectMapper;

	@Inject
	protected TestDataUtil() {}

	@SneakyThrows
	public List<Map<String, String>> readMapsFromBz2Csv(byte[] bytes) {
		var reader = new InputStreamReader(new BZip2CompressorInputStream(new ByteArrayInputStream(bytes)));
		var records = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader).stream()
				.map(r -> r.toMap())
				.collect(Collectors.toList());
		return records;
	}

	@SneakyThrows
	public List<Map<String, String>> readMapsFromJson(InputStream in) {
		var typeFactory = objectMapper.getTypeFactory();
		var map = typeFactory.constructMapType(LinkedHashMap.class, String.class, String.class);
		var list = typeFactory.constructCollectionType(List.class, map);
		return objectMapper.readValue(in, list);
	}

	public byte[] assertRequest(RecordedRequest request, String path) {
		return assertRequest(request, path, null);
	}

	public byte[] assertRequest(RecordedRequest request, String path, String body) {
		assertEquals(path, request.getPath());
		assertEquals("POST", request.getMethod());
		var bodyString = request.getBody().readByteArray();
		if (body == null) {
			assertEquals(0, request.getBodySize());
		} else {
			assertEquals(body, bodyString);
		}
		return bodyString;
	}

	public void assertUserAgent(RecordedRequest request) {
		assertEquals("everef.net", request.getHeader("User-Agent"));
	}

	@SneakyThrows
	public void assertNoMoreRequests(MockWebServer server) {
		assertNull(server.takeRequest(1, TimeUnit.MILLISECONDS));
	}
}
