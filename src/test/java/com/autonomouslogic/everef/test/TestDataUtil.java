package com.autonomouslogic.everef.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.functions.Consumer;
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
}
