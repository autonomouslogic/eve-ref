package com.autonomouslogic.everef.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.mock.MockInterceptor;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.csv.CSVFormat;

@Singleton
public class TestDataUtil {
	@Inject
	ObjectMapper objectMapper;

	@Inject
	MockInterceptor http;

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

	public Response.Builder mockResponse(String url, String body) {
		return mockResponse(url, body.getBytes());
	}

	public Response.Builder mockResponse(String url) {
		return http.addRule().get(url).anyTimes().respond(200);
	}

	public Response.Builder mockResponse(String url, byte[] body) {
		return http.addRule().get(url).anyTimes().respond(body, MediaType.get("application/json"));
	}
}
