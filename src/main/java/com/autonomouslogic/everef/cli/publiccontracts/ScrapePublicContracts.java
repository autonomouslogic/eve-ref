package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.DataIndex;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.mvstore.JsonNodeDataType;
import com.autonomouslogic.everef.mvstore.MVMapRemover;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.S3Util;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.ObjectDataType;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.autonomouslogic.everef.util.ArchivePathFactory.PUBLIC_CONTRACTS;

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
	protected Provider<DataIndex> dataIndexProvider;
	@Inject
	protected JsonNodeDataType jsonNodeDataType;
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
	protected TempFiles tempFiles;

	@Setter
	private ZonedDateTime scrapeTime;

	private S3Url dataUrl;
	private MVStore mvStore;
	private MVMap<Long, JsonNode> contractsStore;
	private MVMap<Long, JsonNode> itemsStore;
	private MVMap<Long, JsonNode> bidsStore;
	private MVMap<Long, JsonNode> dynamicItemsStore;
	private MVMap<Long, JsonNode> nonDynamicItemsStore;
	private MVMap<String, JsonNode> dogmaAttributesStore;
	private MVMap<String, JsonNode> dogmaEffectsStore;

	private ContractsScrapeMeta contractsScrapeMeta;
	private final Set<Long> seenContractIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Inject
	protected ScrapePublicContracts() {
	}

	@Inject
	protected void init() {
		var dataPathUrl = urlParser.parse(Configs.DATA_PATH.getRequired());
		if (!dataPathUrl.getProtocol().equals("s3")) {
			throw new IllegalArgumentException("Data path must be an S3 path");
		}
		dataUrl = (S3Url) dataPathUrl;
		if (!dataUrl.getPath().equals("")) {
			throw new IllegalArgumentException("Data index must be run at the root of the bucket");
		}
	}

	public Completable run() {
		return Completable.concatArray(
				initScrapeTime(),
				initMvStore(),
				initMeta(),
				loadLatestContracts(),
				fetchAndStoreContracts(),
				deleteOldContracts(),
				buildFile().flatMapCompletable(this::uploadFile),
				dataIndexProvider.get().run()
		)
			.doFinally(this::closeMvStore);
	}

	private Completable initScrapeTime() {
		return Completable.fromAction(() -> {
			if (scrapeTime == null) {
				scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
			}
			contractsScrapeMeta.setScrapeStart(scrapeTime.toInstant());
		});
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			log.debug("Opening MVStore");
			mvStore = mvStoreUtil.createTempStore("public-contracts");
			log.debug("MVStore opened at {}", mvStore.getFileStore().getFileName());
			contractsStore = mvStore.openMap("contracts", new MVMap.Builder<Long, JsonNode>()
				.keyType(new ObjectDataType())
				.valueType(jsonNodeDataType)
			);
			itemsStore = mvStore.openMap("items", new MVMap.Builder<Long, JsonNode>()
				.keyType(new ObjectDataType())
				.valueType(jsonNodeDataType)
			);
			bidsStore = mvStore.openMap("bids", new MVMap.Builder<Long, JsonNode>()
				.keyType(new ObjectDataType())
				.valueType(jsonNodeDataType)
			);
			dynamicItemsStore = mvStore.openMap("dynamic_items", new MVMap.Builder<Long, JsonNode>()
				.keyType(new ObjectDataType())
				.valueType(jsonNodeDataType)
			);
			nonDynamicItemsStore = mvStore.openMap("non_dynamic_items", new MVMap.Builder<Long, JsonNode>()
				.keyType(new ObjectDataType())
				.valueType(jsonNodeDataType)
			);
			dogmaAttributesStore = mvStore.openMap("dogma_attributes", new MVMap.Builder<String, JsonNode>()
				.keyType(new ObjectDataType())
				.valueType(jsonNodeDataType)
			);
			dogmaEffectsStore = mvStore.openMap("dogma_effects", new MVMap.Builder<String, JsonNode>()
				.keyType(new ObjectDataType())
				.valueType(jsonNodeDataType)
			);
			log.debug("MVStore initialised");
		});
	}

	private void closeMvStore() {
		try {
			mvStore.close();
		}
		catch (Exception e) {
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
		return Completable.defer(() -> {
			log.warn("TODO loadLatestContracts");
			return Completable.complete(); // @todo
		});
	}

	/**
	 * Fetches all the public contracts and saves them to the MVStore.
	 */
	private Completable fetchAndStoreContracts() {
		return Completable.defer(() -> {
			log.debug("Fetching contracts");
			var start = Instant.now();
			ContractFetcher contractFetcher = contractFetcherProvider.get()
				.setContractsStore(contractsStore)
				.setItemsStore(itemsStore)
				.setBidsStore(bidsStore);
			contractFetcher.getContractAbyssalFetcher()
				.setDynamicItemsStore(dynamicItemsStore)
				.setNonDynamicItemsStore(nonDynamicItemsStore)
				.setDogmaAttributesStore(dogmaAttributesStore)
				.setDogmaEffectsStore(dogmaEffectsStore);

			return contractFetcher.fetchPublicContracts()
				.doOnNext(contractId -> {
					seenContractIds.add(contractId);
				})
				.doOnComplete(() -> {
					contractsScrapeMeta.setScrapeEnd(Instant.now().truncatedTo(ChronoUnit.SECONDS));
					mvStore.commit();
					log.info(String.format("Fetched %s contracts in ", seenContractIds.size(), Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.MILLIS)));
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
			log.debug("Deleting old contracts");
			int removed = new MVMapRemover<>(contractsStore)
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

	private <K> void deleteContractSub(String name, MVMap<K, JsonNode> map) {
		int removed = new MVMapRemover<>(map)
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
		return Single.fromCallable(() -> {
			log.info("Building final file");
			var start = Instant.now();
			var outputFile = tempFiles.tempFile(getClass().getSimpleName(), ".tar.bz2").toFile();
			log.debug(String.format("Writing output file: %s", outputFile));
			var fileBuilder = contractsFileBuilderProvider.get();
			fileBuilder.open(outputFile);
			fileBuilder.writeMeta(contractsScrapeMeta);
			fileBuilder.writeContracts(contractsStore.values());
			fileBuilder.writeItems(itemsStore.values());
			fileBuilder.writeBids(bidsStore.values());
			fileBuilder.writeDynamicItems(dynamicItemsStore.values());
			fileBuilder.writeNonDynamicItems(nonDynamicItemsStore.values());
			fileBuilder.writeDogmaAttributes(dogmaAttributesStore.values());
			fileBuilder.writeDogmaEffects(dogmaEffectsStore.values());
			fileBuilder.close();
			var compressed = CompressUtil.compressBzip2(outputFile);
			log.info(String.format("Final file built in %s", Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.MILLIS)));
			return compressed;
		})
			.compose(Rx.offloadSingle());
	}

	/**
	 * Uploads the final file to S3.
	 * @return
	 */
	private Completable uploadFile(File outputFile) {
		return Completable.defer(() -> {
				var latestPath = S3Url.builder()
					.bucket(dataUrl.getBucket())
					.path(dataUrl.getPath() + PUBLIC_CONTRACTS.createLatestPath())
					.build();
				var archivePath = S3Url.builder()
					.bucket(dataUrl.getBucket())
					.path(dataUrl.getPath() + PUBLIC_CONTRACTS.createArchivePath(scrapeTime))
					.build();
				var latestPut = s3Util.putPublicObjectRequest(outputFile.length(), latestPath, "application/x-bzip2");
				var archivePut = s3Util.putPublicObjectRequest(outputFile.length(), archivePath, "application/x-bzip2");
				log.info(String.format("Uploading latest file to %s", latestPath));
				log.info(String.format("Uploading archive file to %s", archivePath));
				return Completable.mergeArray(
					s3Adapter.putObject(latestPut, outputFile, s3Client).ignoreElement(),
					s3Adapter.putObject(archivePut, outputFile, s3Client).ignoreElement());
		});
	}
}
