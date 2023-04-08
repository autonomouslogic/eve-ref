package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.everef.util.JsonNodeCsvReader;
import com.autonomouslogic.everef.util.Rx;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.h2.mvstore.MVMap;

/**
 * Loads a public contracts archive.
 */
@Log4j2
public class ContractsFileLoader {
	@Inject
	protected JsonNodeCsvReader jsonNodeCsvReader;

	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> contractsStore;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> itemsStore;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> bidsStore;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> dynamicItemsStore;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> nonDynamicItemsStore;

	@Setter
	@NonNull
	private MVMap<String, JsonNode> dogmaEffectsStore;

	@Setter
	@NonNull
	private MVMap<String, JsonNode> dogmaAttributesStore;

	@Inject
	protected ContractsFileLoader() {}

	public Single<ContractsScrapeMeta> loadFile(@NonNull File file) {
		return Single.fromCallable(() -> {
					try (var in = openFile(file)) {
						return loadAllEntries(in);
					}
				})
				.compose(Rx.offloadSingle());
	}

	@SneakyThrows
	private TarArchiveInputStream openFile(@NonNull File file) {
		log.debug("Reading file from {}", file);
		InputStream in = new FileInputStream(file);
		if (file.getPath().endsWith(".bz2")) {
			log.trace("Reading bzip2 file");
			in = new BZip2CompressorInputStream(in);
		}
		log.trace("Reading tar file");
		return new TarArchiveInputStream(in);
	}

	@SneakyThrows
	private ContractsScrapeMeta loadAllEntries(@NonNull TarArchiveInputStream tar) {
		TarArchiveEntry entry;
		ContractsScrapeMeta meta = null;
		while ((entry = tar.getNextTarEntry()) != null) {
			if (entry.isDirectory()) {
				continue;
			}
			var file = entry.getName();
			switch (file) {
				case "":
					break;
				case ContractsFileBuilder.META_JSON:
					meta = loadMeta(tar);
					break;
				case ContractsFileBuilder.CONTRACTS_CSV:
					loadContracts(tar);
					break;
				case ContractsFileBuilder.ITEMS_CSV:
					loadItems(tar);
					break;
				case ContractsFileBuilder.BIDS_CSV:
					loadBids(tar);
					break;
				case ContractsFileBuilder.DYNAMIC_ITEMS_CSV:
					loadDynamicItems(tar);
					break;
				case ContractsFileBuilder.NON_DYNAMIC_ITEMS_CSV:
					loadNonDynamicItems(tar);
					break;
				case ContractsFileBuilder.DOGMA_ATTRIBUTES_CSV:
					loadDogmaAttributes(tar);
					break;
				case ContractsFileBuilder.DOGMA_EFFECTS_CSV:
					loadDogmaEffects(tar);
					break;
				default:
					log.warn("Unknown entry in file: {}", file);
					break;
			}
		}
		return meta;
	}

	@SneakyThrows
	public ContractsScrapeMeta loadMeta(@NonNull InputStream in) {
		log.debug("Reading meta");
		return objectMapper
				.copy()
				.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
				.readValue(in, ContractsScrapeMeta.class);
	}

	@SneakyThrows
	public void loadContracts(@NonNull InputStream in) {
		log.debug("Reading contracts");
		loadEntries(in, contractsStore, ContractsFileBuilder.CONTRACT_ID);
	}

	@SneakyThrows
	public void loadItems(@NonNull InputStream in) {
		log.debug("Reading items");
		loadEntries(in, itemsStore, ContractsFileBuilder.ITEM_ID);
	}

	@SneakyThrows
	public void loadBids(@NonNull InputStream in) {
		log.debug("Reading bids");
		loadEntries(in, bidsStore, ContractsFileBuilder.BID_ID);
	}

	@SneakyThrows
	public void loadDynamicItems(@NonNull InputStream in) {
		log.debug("Reading dynamic items");
		loadEntries(in, dynamicItemsStore, ContractsFileBuilder.DYNAMIC_ITEM_ID);
	}

	@SneakyThrows
	public void loadNonDynamicItems(@NonNull InputStream in) {
		log.debug("Reading non-dynamic items");
		loadEntries(in, nonDynamicItemsStore, ContractsFileBuilder.NON_DYNAMIC_ITEM_ID);
	}

	@SneakyThrows
	public void loadDogmaAttributes(@NonNull InputStream in) {
		log.debug("Reading dogma atributes");
		loadEntries(in, dogmaAttributesStore, ContractsFileBuilder.DOGMA_ATTRIBUTE_ID);
	}

	@SneakyThrows
	public void loadDogmaEffects(@NonNull InputStream in) {
		log.debug("Reading dogma effects");
		loadEntries(in, dogmaEffectsStore, ContractsFileBuilder.DOGMA_EFFECT_ID);
	}

	private <K> void loadEntries(
			@NonNull InputStream in, @NonNull MVMap<K, JsonNode> store, @NonNull Function<JsonNode, K> idExtractor) {
		jsonNodeCsvReader.readAll(in).forEach(entry -> store.put(idExtractor.apply(entry), entry));
	}
}
