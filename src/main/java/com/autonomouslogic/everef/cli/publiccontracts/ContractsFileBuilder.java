package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.everef.util.JsonNodeCsvWriter;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * Builds the public contract distribution files.
 */
@Log4j2
public class ContractsFileBuilder {
	@Inject
	protected TempFiles tempFiles;

	private final ObjectMapper objectMapper;

	private final long modTime = System.currentTimeMillis();

	private TarArchiveOutputStream tar;

	@Inject
	protected ContractsFileBuilder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper.copy().disable(SerializationFeature.CLOSE_CLOSEABLE);
	}

	@SneakyThrows
	public void open(File file) {
		log.debug(String.format("Opening file for tar output: %s", file));
		tar = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	}

	@SneakyThrows
	public void writeMeta(ContractsScrapeMeta meta) {
		log.debug("Writing meta");
		writeEntry("meta.json", objectMapper.writeValueAsBytes(meta));
	}

	@SneakyThrows
	public void writeContracts(Collection<JsonNode> contracts) {
		log.debug(String.format("Writing %s contracts", contracts.size()));
		writeEntries(contracts, "contracts.csv");
	}

	@SneakyThrows
	public void writeItems(Collection<JsonNode> items) {
		log.debug(String.format("Writing %s items", items.size()));
		writeEntries(items, "contract_items.csv");
	}

	@SneakyThrows
	public void writeBids(Collection<JsonNode> bids) {
		log.debug(String.format("Writing %s bids", bids.size()));
		writeEntries(bids, "contract_bids.csv");
	}

	@SneakyThrows
	public void writeDynamicItems(Collection<JsonNode> dynamicItems) {
		log.debug(String.format("Writing %s dynamicItems", dynamicItems.size()));
		writeEntries(dynamicItems, "contract_dynamic_items.csv");
	}

	@SneakyThrows
	public void writeNonDynamicItems(Collection<JsonNode> nonDynamicItems) {
		log.debug(String.format("Writing %s nonDynamicItems", nonDynamicItems.size()));
		writeEntries(nonDynamicItems, "contract_non_dynamic_items.csv");
	}

	@SneakyThrows
	public void writeDogmaAttributes(Collection<JsonNode> dogmaAttributes) {
		log.debug(String.format("Writing %s dogmaAttributes", dogmaAttributes.size()));
		writeEntries(dogmaAttributes, "contract_dynamic_items_dogma_attributes.csv");
	}

	@SneakyThrows
	public void writeDogmaEffects(Collection<JsonNode> dogmaEffects) {
		log.debug(String.format("Writing %s dogmaEffects", dogmaEffects.size()));
		writeEntries(dogmaEffects, "contract_dynamic_items_dogma_effects.csv");
	}

	@SneakyThrows
	private void writeEntries(Iterable<JsonNode> entries, String filename) {
		var file =
				tempFiles.tempFile(getClass().getSimpleName(), "-" + filename).toFile();
		new JsonNodeCsvWriter().setOut(file).writeAll(entries);
		writeEntry(filename, file.length(), new FileInputStream(file));
	}

	@SneakyThrows
	private void writeEntry(String name, byte[] data) {
		writeEntry(name, data.length, new ByteArrayInputStream(data));
	}

	@SneakyThrows
	private void writeEntry(String name, long length, InputStream in) {
		var entry = new TarArchiveEntry(name);
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
