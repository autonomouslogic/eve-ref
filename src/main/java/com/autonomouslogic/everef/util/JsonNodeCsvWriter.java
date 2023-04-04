package com.autonomouslogic.everef.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for taking a list of JsonNode objects and converting them into a CSV file.
 */
public class JsonNodeCsvWriter {
	@Getter
	@Setter
	@NonNull
	private CSVFormat csvFormat = CSVFormat.RFC4180;

	@Getter
	@Setter
	@NonNull
	private File out;

	@SneakyThrows
	public void writeAll(@NonNull Iterable<JsonNode> entries) {
		var headers = getHeaders(entries);
		var printer = csvFormat.withHeader(headers.toArray(new String[0])).print(out, StandardCharsets.UTF_8);
		writeAll(printer, entries, headers);
		printer.close();
	}

	@SneakyThrows
	private void writeAll(
			@NonNull CSVPrinter printer, @NonNull Iterable<JsonNode> entries, @NonNull List<String> headers) {
		var record = new ArrayList<String>();
		for (var entry : entries) {
			if (entry instanceof ObjectNode) {
				var obj = (ObjectNode) entry;
				record.clear();
				writeEntry(printer, headers, obj);
			}
		}
	}

	private void writeEntry(@NotNull CSVPrinter printer, @NotNull List<String> headers, @NonNull ObjectNode obj) throws IOException {
		var record = new ArrayList<String>(headers.size());
		for (String header : headers) {
			if (obj.has(header)) {
				var val = valueToString(obj.get(header));
				record.add(val);
			} else {
				record.add("");
			}
		}
		printer.printRecord(record);
	}

	private String valueToString(@NonNull JsonNode value) {
		if (value.isNumber()) {
			var number = value.numberValue();
			if (number instanceof BigDecimal) {
				return ((BigDecimal) number).toPlainString();
			}
		}
		return value.asText();
	}

	private List<String> getHeaders(@NonNull Iterable<JsonNode> entries) {
		var headers = new LinkedHashSet<String>();
		for (var entry : entries) {
			if (entry instanceof ObjectNode) {
				entry.fieldNames().forEachRemaining(headers::add);
			}
		}
		return new ArrayList<>(headers);
	}
}
