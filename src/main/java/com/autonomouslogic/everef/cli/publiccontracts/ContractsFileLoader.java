package com.autonomouslogic.everef.cli.publiccontracts;

import static com.autonomouslogic.everef.util.ArchivePathFactory.PUBLIC_CONTRACTS;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.JsonNodeCsvReader;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Maybe;
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
import okhttp3.OkHttpClient;
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

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

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

	public Maybe<ContractsScrapeMeta> downloadAndLoad() {
		return downloadLatestFile().flatMapSingle(this::loadFile);
	}

	/**
	 * Downloads the latest contracts file from the EVE Ref data site.
	 */
	public Maybe<File> downloadLatestFile() {
		return Maybe.defer(() -> {
					log.info("Downloading latest contracts");
					var baseUrl = Configs.DATA_BASE_URL.getRequired();
					var url = baseUrl + "/" + PUBLIC_CONTRACTS.createLatestPath();
					var file = tempFiles
							.tempFile("public-contracts-latest", ".tar.bz2")
							.toFile();
					return okHttpHelper.download(url, file, okHttpClient).flatMapMaybe(response -> {
						if (response.code() != 200) {
							log.warn("Failed loading latest contracts: HTTP {}, ignoring", response.code());
							return Maybe.empty();
						}
						return Maybe.just(file);
					});
				})
				.onErrorResumeNext(e -> {
					log.warn("Failed reading latest contracts, ignoring", e);
					return Maybe.empty();
				});
	}

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
		loadEntries("contracts", in, contractsStore, ContractsFileBuilder.CONTRACT_ID);
	}

	@SneakyThrows
	public void loadItems(@NonNull InputStream in) {
		log.debug("Reading items");
		loadEntries("items", in, itemsStore, ContractsFileBuilder.ITEM_ID);
	}

	@SneakyThrows
	public void loadBids(@NonNull InputStream in) {
		log.debug("Reading bids");
		loadEntries("bids", in, bidsStore, ContractsFileBuilder.BID_ID);
	}

	@SneakyThrows
	public void loadDynamicItems(@NonNull InputStream in) {
		log.debug("Reading dynamic items");
		loadEntries("dynamic items", in, dynamicItemsStore, ContractsFileBuilder.DYNAMIC_ITEM_ID);
	}

	@SneakyThrows
	public void loadNonDynamicItems(@NonNull InputStream in) {
		log.debug("Reading non-dynamic items");
		loadEntries("non-dynamic items", in, nonDynamicItemsStore, ContractsFileBuilder.NON_DYNAMIC_ITEM_ID);
	}

	@SneakyThrows
	public void loadDogmaAttributes(@NonNull InputStream in) {
		log.debug("Reading dogma attributes");
		loadEntries("dogma attributes", in, dogmaAttributesStore, ContractsFileBuilder.DOGMA_ATTRIBUTE_ID);
	}

	@SneakyThrows
	public void loadDogmaEffects(@NonNull InputStream in) {
		log.debug("Reading dogma effects");
		loadEntries("dogma effects", in, dogmaEffectsStore, ContractsFileBuilder.DOGMA_EFFECT_ID);
	}

	private <K> void loadEntries(@NonNull String name,
			@NonNull InputStream in, @NonNull MVMap<K, JsonNode> store, @NonNull Function<JsonNode, K> idExtractor) {
		try {
			jsonNodeCsvReader.readAll(in).forEach(entry -> store.put(idExtractor.apply(entry), entry));
		}
		catch (Exception e) {
			log.warn("Failed loading {}, ignoring", name, e);
		}
	}
}
