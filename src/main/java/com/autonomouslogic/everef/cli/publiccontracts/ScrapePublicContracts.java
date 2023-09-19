package com.autonomouslogic.everef.cli.publiccontracts;

import static com.autonomouslogic.everef.util.ArchivePathFactory.PUBLIC_CONTRACTS;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.mvstore.MapRemover;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.S3Util;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.h2.mvstore.MVStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Fetches and stores all public contracts.
 */
@Log4j2
public class ScrapePublicContracts implements Command {
	@Inject
	protected Provider<ContractFetcher> contractFetcherProvider;

	@Inject
	protected Provider<ContractsFileBuilder> contractsFileBuilderProvider;

	@Inject
	protected Provider<ContractsFileLoader> contractsFileLoaderProvider;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected S3Util s3Util;

	@Inject
	@Named("data")
	protected S3AsyncClient s3Client;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected TempFiles tempFiles;

	@Setter
	private ZonedDateTime scrapeTime;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	private S3Url dataUrl;
	private MVStore mvStore;
	private Map<Long, JsonNode> contractsStore;
	private Map<Long, JsonNode> itemsStore;
	private Map<Long, JsonNode> bidsStore;
	private Map<Long, JsonNode> dynamicItemsStore;
	private Map<Long, JsonNode> nonDynamicItemsStore;
	private Map<String, JsonNode> dogmaAttributesStore;
	private Map<String, JsonNode> dogmaEffectsStore;

	private ContractsScrapeMeta contractsScrapeMeta;
	private final Set<Long> seenContractIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Inject
	protected ScrapePublicContracts() {}

	@Inject
	protected void init() {
		dataUrl = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
	}

	public Completable run() {
		return Completable.concatArray(
						initScrapeTime(),
						initMvStore(),
						initMeta(),
						loadLatestContracts(),
						fetchAndStoreContracts(),
						deleteOldContracts(),
						buildFile().flatMapCompletable(this::uploadFiles))
				.doFinally(this::closeMvStore);
	}

	private Completable initScrapeTime() {
		return Completable.fromAction(() -> {
			if (scrapeTime == null) {
				scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
			}
			contractsScrapeMeta = new ContractsScrapeMeta().setScrapeStart(scrapeTime.toInstant());
		});
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			log.info("Opening MVStore");
			mvStore = mvStoreUtil.createTempStore("public-contracts");
			log.debug("MVStore opened at {}", mvStore.getFileStore().getFileName());
			contractsStore = mvStoreUtil.openJsonMap(mvStore, "contracts", Long.class);
			itemsStore = mvStoreUtil.openJsonMap(mvStore, "items", Long.class);
			bidsStore = mvStoreUtil.openJsonMap(mvStore, "bids", Long.class);
			dynamicItemsStore = mvStoreUtil.openJsonMap(mvStore, "dynamic_items", Long.class);
			nonDynamicItemsStore = mvStoreUtil.openJsonMap(mvStore, "non_dynamic_items", Long.class);
			dogmaAttributesStore = mvStoreUtil.openJsonMap(mvStore, "dogma_attributes", String.class);
			dogmaEffectsStore = mvStoreUtil.openJsonMap(mvStore, "dogma_effects", String.class);
			log.debug("MVStore initialised");
		});
	}

	private void closeMvStore() {
		try {
			mvStore.close();
		} catch (Exception e) {
			log.debug("Failed closing MVStore, ignoring");
		}
	}

	@SneakyThrows
	private Completable initMeta() {
		return Completable.fromAction(() -> {
			contractsScrapeMeta = new ContractsScrapeMeta()
					.setDatasource(Configs.ESI_DATASOURCE.getRequired())
					.setScrapeStart(scrapeTime.truncatedTo(ChronoUnit.SECONDS).toInstant());
		});
	}

	/**
	 * Loads the latest contract scrape file and imports it into the MVStore.
	 */
	private Completable loadLatestContracts() {
		return Completable.defer(() -> contractsFileLoaderProvider
				.get()
				.setContractsStore(contractsStore)
				.setItemsStore(itemsStore)
				.setBidsStore(bidsStore)
				.setDynamicItemsStore(dynamicItemsStore)
				.setNonDynamicItemsStore(nonDynamicItemsStore)
				.setDogmaAttributesStore(dogmaAttributesStore)
				.setDogmaEffectsStore(dogmaEffectsStore)
				.downloadAndLoad()
				.doOnSuccess(meta -> {
					var age = Duration.between(meta.getScrapeStart(), Instant.now());
					if (age.compareTo(Duration.ofHours(1)) > 0) {
						log.warn("Downloaded latest contracts is old: {}", age);
					}
					log.info("Loaded latest contracts from scrape starting: {} - age: {}", meta.getScrapeStart(), age);
				})
				.ignoreElement()
				.onErrorResumeNext(e -> {
					log.warn("Failed reading latest contracts, ignoring", e);
					return Completable.complete();
				}));
	}

	/**
	 * Fetches all the public contracts and saves them to the MVStore.
	 */
	private Completable fetchAndStoreContracts() {
		return Completable.defer(() -> {
			log.info("Fetching contracts");
			var start = Instant.now();
			var contractFetcher = contractFetcherProvider
					.get()
					.setContractsStore(contractsStore)
					.setItemsStore(itemsStore)
					.setBidsStore(bidsStore);
			var abyssalFetcher = contractFetcher.getContractAbyssalFetcher();
			abyssalFetcher
					.setDynamicItemsStore(dynamicItemsStore)
					.setNonDynamicItemsStore(nonDynamicItemsStore)
					.setDogmaAttributesStore(dogmaAttributesStore)
					.setDogmaEffectsStore(dogmaEffectsStore);

			return contractFetcher
					.fetchPublicContracts()
					.doOnNext(contractId -> {
						seenContractIds.add(contractId);
					})
					.doOnComplete(() -> {
						contractsScrapeMeta.setScrapeEnd(Instant.now().truncatedTo(ChronoUnit.SECONDS));
						mvStore.commit();
						log.info(String.format(
								"Fetched %s contracts in %s",
								seenContractIds.size(),
								Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.MILLIS)));
					})
					.ignoreElements();
		});
	}

	/**
	 * Deletes contracts from the database which don't exist anymore.
	 * @return
	 */
	private Completable deleteOldContracts() {
		return Completable.fromAction(() -> {
			log.info("Deleting old contracts");
			int removed = new MapRemover<>(contractsStore)
					.removeIf((contractId, contract) -> !seenContractIds.contains(contractId))
					.getEntriesRemoved();
			log.debug(String.format("Deleted %s old contracts", removed));
			deleteContractSub("items", itemsStore);
			deleteContractSub("bids", bidsStore);
			deleteContractSub("dynamic items", dynamicItemsStore);
			deleteContractSub("non dynamic items", nonDynamicItemsStore);
			deleteContractSub("dogma attributes", dogmaAttributesStore);
			deleteContractSub("dogma effects", dogmaEffectsStore);
		});
	}

	private <K> void deleteContractSub(String name, Map<K, JsonNode> map) {
		var removed = new MapRemover<>(map)
				.removeIf((id, contract) -> {
					long contractId = contract.get("contract_id").asLong();
					return !seenContractIds.contains(contractId);
				})
				.getEntriesRemoved();
		log.debug(String.format("Deleted %s old contract %s", removed, name));
	}

	/**
	 * Builds the output file.
	 */
	private Single<File> buildFile() {
		return Single.defer(() -> contractsFileBuilderProvider
				.get()
				.setContractsScrapeMeta(contractsScrapeMeta)
				.setContractsStore(contractsStore)
				.setItemsStore(itemsStore)
				.setBidsStore(bidsStore)
				.setDynamicItemsStore(dynamicItemsStore)
				.setNonDynamicItemsStore(nonDynamicItemsStore)
				.setDogmaAttributesStore(dogmaAttributesStore)
				.setDogmaEffectsStore(dogmaEffectsStore)
				.buildFile());
	}

	/**
	 * Uploads the final file to S3.
	 * @return
	 */
	private Completable uploadFiles(File outputFile) {
		return Completable.defer(() -> {
			var latestPath = dataUrl.resolve(PUBLIC_CONTRACTS.createLatestPath());
			var archivePath = dataUrl.resolve(PUBLIC_CONTRACTS.createArchivePath(scrapeTime));
			var latestPut = s3Util.putPublicObjectRequest(
					outputFile.length(), latestPath, "application/x-bzip2", latestCacheTime);
			var archivePut = s3Util.putPublicObjectRequest(
					outputFile.length(), archivePath, "application/x-bzip2", archiveCacheTime);
			log.info(String.format("Uploading latest file to %s", latestPath));
			log.info(String.format("Uploading archive file to %s", archivePath));
			return Completable.mergeArray(
					s3Adapter.putObject(latestPut, outputFile, s3Client).ignoreElement(),
					s3Adapter.putObject(archivePut, outputFile, s3Client).ignoreElement());
		});
	}
}
