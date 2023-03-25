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
}
