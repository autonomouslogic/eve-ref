package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.FormatUtil;
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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
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
	public static final String META_JSON = "meta.json";
	public static final String CONTRACTS_CSV = "contracts.csv";
	public static final String ITEMS_CSV = "contract_items.csv";
	public static final String BIDS_CSV = "contract_bids.csv";
	public static final String DYNAMIC_ITEMS_CSV = "contract_dynamic_items.csv";
	public static final String NON_DYNAMIC_ITEMS_CSV = "contract_non_dynamic_items.csv";
	public static final String DOGMA_ATTRIBUTES_CSV = "contract_dynamic_items_dogma_attributes.csv";
	public static final String DOGMA_EFFECTS_CSV = "contract_dynamic_items_dogma_effects.csv";

	public static final Function<JsonNode, Long> CONTRACT_ID =
			node -> node.get("contract_id").asLong();
	public static final Function<JsonNode, Long> ITEM_ID =
			node -> node.get("record_id").asLong();
	public static final Function<JsonNode, Long> BID_ID =
			node -> node.get("bid_id").asLong();
	public static final Function<JsonNode, Long> DYNAMIC_ITEM_ID =
			node -> node.get("item_id").asLong();
	public static final Function<JsonNode, Long> NON_DYNAMIC_ITEM_ID =
			node -> node.get("item_id").asLong();
	public static final Function<JsonNode, String> DOGMA_ATTRIBUTE_ID =
			node -> FormatUtil.toHexString(node.get("item_id").asLong()) + "-"
					+ FormatUtil.toHexString(node.get("attribute_id").asLong());
	public static final Function<JsonNode, String> DOGMA_EFFECT_ID =
			node -> FormatUtil.toHexString(node.get("item_id").asLong()) + "-"
					+ FormatUtil.toHexString(node.get("effect_id").asLong());

	@Inject
	protected TempFiles tempFiles;

	private final ObjectMapper objectMapper;

	private final long modTime = System.currentTimeMillis();

	@Setter
	@NonNull
	private ContractsScrapeMeta contractsScrapeMeta;

	@Setter
	@NonNull
	private Map<Long, JsonNode> contractsStore;

	@Setter
	@NonNull
	private Map<Long, JsonNode> itemsStore;

	@Setter
	@NonNull
	private Map<Long, JsonNode> bidsStore;

	@Setter
	@NonNull
	private Map<Long, JsonNode> dynamicItemsStore;

	@Setter
	@NonNull
	private Map<Long, JsonNode> nonDynamicItemsStore;

	@Setter
	@NonNull
	private Map<String, JsonNode> dogmaEffectsStore;

	@Setter
	@NonNull
	private Map<String, JsonNode> dogmaAttributesStore;

	private TarArchiveOutputStream tar;

	@Inject
	protected ContractsFileBuilder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper.copy().disable(SerializationFeature.CLOSE_CLOSEABLE);
	}

	/**
	 * Builds the output file.
	 */
	public File buildFile() {
		log.info("Building final file");
		var start = Instant.now();
		var outputFile = tempFiles.tempFile(getClass().getSimpleName(), ".tar").toFile();
		log.debug(String.format("Writing output file: %s", outputFile));
		open(outputFile);
		writeMeta(contractsScrapeMeta);
		writeContracts(contractsStore.values());
		writeItems(itemsStore.values());
		writeBids(bidsStore.values());
		writeDynamicItems(dynamicItemsStore.values());
		writeNonDynamicItems(nonDynamicItemsStore.values());
		writeDogmaAttributes(dogmaAttributesStore.values());
		writeDogmaEffects(dogmaEffectsStore.values());
		close();
		var compressed = CompressUtil.compressBzip2(outputFile);
		compressed.deleteOnExit();
		log.info(String.format(
				"Final file built in %s", Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.MILLIS)));
		return compressed;
	}

	@SneakyThrows
	private void open(File file) {
		log.debug(String.format("Opening file for tar output: %s", file));
		tar = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	}

	@SneakyThrows
	private void writeMeta(ContractsScrapeMeta meta) {
		log.debug("Writing meta");
		writeEntry(META_JSON, objectMapper.writeValueAsBytes(meta));
	}

	@SneakyThrows
	private void writeContracts(Collection<JsonNode> contracts) {
		log.debug(String.format("Writing %s contracts", contracts.size()));
		writeEntries(contracts, CONTRACTS_CSV);
	}

	@SneakyThrows
	private void writeItems(Collection<JsonNode> items) {
		log.debug(String.format("Writing %s items", items.size()));
		writeEntries(items, ITEMS_CSV);
	}

	@SneakyThrows
	private void writeBids(Collection<JsonNode> bids) {
		log.debug(String.format("Writing %s bids", bids.size()));
		writeEntries(bids, BIDS_CSV);
	}

	@SneakyThrows
	private void writeDynamicItems(Collection<JsonNode> dynamicItems) {
		log.debug(String.format("Writing %s dynamicItems", dynamicItems.size()));
		writeEntries(dynamicItems, DYNAMIC_ITEMS_CSV);
	}

	@SneakyThrows
	private void writeNonDynamicItems(Collection<JsonNode> nonDynamicItems) {
		log.debug(String.format("Writing %s nonDynamicItems", nonDynamicItems.size()));
		writeEntries(nonDynamicItems, NON_DYNAMIC_ITEMS_CSV);
	}

	@SneakyThrows
	private void writeDogmaAttributes(Collection<JsonNode> dogmaAttributes) {
		log.debug(String.format("Writing %s dogmaAttributes", dogmaAttributes.size()));
		writeEntries(dogmaAttributes, DOGMA_ATTRIBUTES_CSV);
	}

	@SneakyThrows
	private void writeDogmaEffects(Collection<JsonNode> dogmaEffects) {
		log.debug(String.format("Writing %s dogmaEffects", dogmaEffects.size()));
		writeEntries(dogmaEffects, DOGMA_EFFECTS_CSV);
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
	private void close() {
		tar.close();
	}
}
