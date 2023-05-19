package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.JsonNodeCsvWriter;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Ordering;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MarketHistoryFileBuilder {
	@Inject
	protected TempFiles tempFiles;

	private static final Ordering<JsonNode> REGION_ORDERING =
			Ordering.natural().onResultOf(json -> json.get("region_id").asLong());
	private static final Ordering<JsonNode> TYPE_ORDERING =
			Ordering.natural().onResultOf(json -> json.get("type_id").asLong());
	private static final Ordering<JsonNode> ENTRY_ORDERING =
			Ordering.compound(Arrays.asList(REGION_ORDERING, TYPE_ORDERING));

	@Inject
	protected MarketHistoryFileBuilder() {}

	@SneakyThrows
	public File writeEntries(Collection<JsonNode> entries) {
		var values = new ArrayList<>(entries);
		values.sort(ENTRY_ORDERING);
		// Write to file.
		var csv = tempFiles.tempFile("market-history", ".csv").toFile();
		csv.deleteOnExit();
		new JsonNodeCsvWriter().setOut(csv).writeAll(values);
		// Compress.
		var compressed = CompressUtil.compressBzip2(csv);
		compressed.deleteOnExit();

		verifyLineCount(compressed, entries.size() + 1);

		return compressed;
	}

	private void verifyLineCount(File file, int expected) {
		var actual = CompressUtil.lineCount(file);
		if (actual != expected) {
			throw new IllegalStateException(String.format("Expected %s lines in %s, %s found", expected, file, actual));
		}
	}
}
