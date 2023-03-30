package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.evemarket.scrape.JsonNodeCsvBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Builds the public contract distribution files.
 */
@Slf4j
public class ContractsFileBuilder {
	private ObjectMapper objectMapper;

	private long modTime = System.currentTimeMillis();

	private BZip2CompressorOutputStream bzip;
	private TarArchiveOutputStream tar;

	@Inject
	protected ContractsFileBuilder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper.copy()
			.disable(SerializationFeature.CLOSE_CLOSEABLE);
	}

	@SneakyThrows
	public void open(File file) {
		// Main output file.
		OutputStream fout = new FileOutputStream(file);
		bzip = new BZip2CompressorOutputStream(fout);
		tar = new TarArchiveOutputStream(bzip);
	}

	@SneakyThrows
	public void writeMeta(ContractsScrapeMeta meta) {
		writeEntry("meta.json", objectMapper.writeValueAsBytes(meta));
	}

	@SneakyThrows
	public void writeContracts(Iterable<JsonNode> contracts) {
		writeEntries(contracts, "contracts.csv");
	}

	@SneakyThrows
	public void writeItems(Iterable<JsonNode> items) {
		writeEntries(items, "contract_items.csv");
	}

	@SneakyThrows
	public void writeBids(Iterable<JsonNode> bids) {
		writeEntries(bids, "contract_bids.csv");
	}

	@SneakyThrows
	public void writeDynamicItems(Iterable<JsonNode> bids) {
		writeEntries(bids, "contract_dynamic_items.csv");
	}

	@SneakyThrows
	public void writeDogmaAttributes(Iterable<JsonNode> bids) {
		writeEntries(bids, "contract_dynamic_items_dogma_attributes.csv");
	}

	@SneakyThrows
	public void writeDogmaEffects(Iterable<JsonNode> bids) {
		writeEntries(bids, "contract_dynamic_items_dogma_effects.csv");
	}

	@SneakyThrows
	private void writeEntries(Iterable<JsonNode> bids, String filename) {
		File file = Files.createTempFile(getClass().getSimpleName(), "-" + filename).toFile();
		file.deleteOnExit();
		new JsonNodeCsvBuilder()
			.setOut(file)
			.writeAll(bids);
		writeEntry(filename, file.length(), new FileInputStream(file));
	}

	@SneakyThrows
	private void writeEntry(String name, byte[] data) {
		writeEntry(name, data.length, new ByteArrayInputStream(data));
	}

	@SneakyThrows
	private void writeEntry(String name, long length, InputStream in) {
		TarArchiveEntry entry = new TarArchiveEntry(name);
		entry.setModTime(modTime);
		entry.setSize(length);
		tar.putArchiveEntry(entry);
		IOUtils.copy(in, tar);
		tar.closeArchiveEntry();
	}

	@SneakyThrows
	public void close() {
		tar.close();
	}
}
