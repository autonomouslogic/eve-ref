package com.autonomouslogic.everef.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;

/**
 * Utility class for reading a CSV file into JsonNodes.
 */
public class JsonNodeCsvReader {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected JsonNodeCsvReader() {}

	@SneakyThrows
	public Stream<JsonNode> readAll(InputStream in) {
		return CSVFormat.RFC4180
				.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.build()
				.parse(new InputStreamReader(in, StandardCharsets.UTF_8))
				.stream()
				.map(r -> {
					var json = objectMapper.createObjectNode();
					r.toMap().forEach(json::put);
					return json;
				});
	}

	@SneakyThrows
	public Stream<JsonNode> readAll(byte[] bytes) {
		return readAll(new ByteArrayInputStream(bytes));
	}

	@SneakyThrows
	public Flowable<JsonNode> readCompressed(File file) {
		return Flowable.defer(() -> {
					var in = CompressUtil.uncompress(file);
					return Flowable.fromStream(readAll(in)).doFinally(in::close);
				})
				.compose(Rx.offloadFlowable());
	}
}
