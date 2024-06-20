package com.autonomouslogic.everef.cli.structures;

import static com.autonomouslogic.everef.util.ArchivePathFactory.STRUCTURES;
import static com.autonomouslogic.everef.util.EveConstants.STANDARD_MARKET_HUB_I_TYPE_ID;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.structures.source.Adam4EveBackfillStructureSource;
import com.autonomouslogic.everef.cli.structures.source.BackfillPublicStructureSource;
import com.autonomouslogic.everef.cli.structures.source.MarketOrdersStructureSource;
import com.autonomouslogic.everef.cli.structures.source.OldStructureSource;
import com.autonomouslogic.everef.cli.structures.source.PublicContractsStructureSource;
import com.autonomouslogic.everef.cli.structures.source.PublicStructureSource;
import com.autonomouslogic.everef.cli.structures.source.SirSmashAlotBackfillStructureSource;
import com.autonomouslogic.everef.cli.structures.source.SovereigntyStructureSource;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.OwnerPopulator;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.openapi.refdata.apis.RefdataApi;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.HttpUrl;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.JsonUtil;
import com.autonomouslogic.everef.util.ProgressReporter;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.h2.mvstore.MVStore;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Scrapes structure information.
 */
@Log4j2
public class ScrapeStructures implements Command {
	public static final String STRUCTURE_ID = "structure_id";
	public static final String IS_GETTABLE_STRUCTURE = "is_gettable_structure";
	public static final String LAST_STRUCTURE_GET = "last_structure_get";
	public static final String IS_PUBLIC_STRUCTURE = "is_public_structure";
	public static final String LAST_SEEN_PUBLIC_STRUCTURE = "last_seen_public_structure";
	public static final String IS_MARKET_STRUCTURE = "is_market_structure";
	public static final String LAST_SEEN_MARKET_STRUCTURE = "last_seen_market_structure";
	public static final String FIRST_SEEN = "first_seen";
	public static final String OWNER_ID = "owner_id";
	public static final String OWNER_CORPORATION_ID = "owner_alliance_id";
	public static final String OWNER_CORPORATION_NAME = "owner_alliance_name";
	public static final String OWNER_ALLIANCE_ID = "owner_alliance_id";
	public static final String OWNER_ALLIANCE_NAME = "owner_alliance_name";

	@Deprecated
	public static final String IS_SOVEREIGNTY_STRUCTURE = "is_sovereignty_structure";

	@Deprecated
	public static final String LAST_SEEN_SOVEREIGNTY_STRUCTURE = "last_seen_sovereignty_structure";

	public static final List<String> ALL_CUSTOM_PROPERTIES = List.of(
			STRUCTURE_ID,
			IS_GETTABLE_STRUCTURE,
			LAST_STRUCTURE_GET,
			IS_PUBLIC_STRUCTURE,
			LAST_SEEN_PUBLIC_STRUCTURE,
			IS_MARKET_STRUCTURE,
			LAST_SEEN_MARKET_STRUCTURE,
			FIRST_SEEN,
			"constellation_id",
			"region_id",
		OWNER_CORPORATION_ID,
		OWNER_CORPORATION_NAME,OWNER_ALLIANCE_ID,OWNER_ALLIANCE_NAME);

	public static final List<String> ALL_BOOLEANS =
			List.of(IS_GETTABLE_STRUCTURE, IS_PUBLIC_STRUCTURE, IS_MARKET_STRUCTURE);

	public static final List<String> ALL_TIMESTAMPS =
			List.of(LAST_STRUCTURE_GET, LAST_SEEN_PUBLIC_STRUCTURE, LAST_SEEN_MARKET_STRUCTURE, FIRST_SEEN);

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected ObjectMapper objectMapper;

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

	@Inject
	protected DataIndexHelper dataIndexHelper;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected UniverseApi universeApi;

	@Inject
	protected EsiAuthHelper esiAuthHelper;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected LocationPopulator locationPopulator;

	@Inject
	protected OwnerPopulator ownerPopulator;

	@Inject
	protected StructureScrapeHelper structureScrapeHelper;

	@Inject
	protected StructureStore structureStore;

	@Inject
	protected OldStructureSource oldStructureSource;

	@Inject
	protected BackfillPublicStructureSource backfillPublicStructureSource;

	@Inject
	protected Adam4EveBackfillStructureSource adam4EveBackfillStructureSource;

	@Inject
	protected SirSmashAlotBackfillStructureSource sirSmashAlotBackfillStructureSource;

	@Inject
	protected PublicStructureSource publicStructureSource;

	@Inject
	protected MarketOrdersStructureSource marketOrdersStructureSource;

	@Inject
	protected PublicContractsStructureSource publicContractsStructureSource;

	@Inject
	protected SovereigntyStructureSource sovereigntyStructureSource;

	@Inject
	protected RefdataApi refdataApi;

	@Setter
	private ZonedDateTime scrapeTime;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private final String scrapeOwnerHash = Configs.SCRAPE_CHARACTER_OWNER_HASH.getRequired();
	private final Duration structureTimeout = Configs.STRUCTURE_TIMEOUT.getRequired();
	private S3Url dataPath;
	private HttpUrl dataUrl;
	private MVStore mvStore;
	private List<Long> marketStructureTypeIds;
	private ProgressReporter progressReporter;

	@Inject
	protected ScrapeStructures() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		dataUrl = (HttpUrl) urlParser.parse(Configs.DATA_BASE_URL.getRequired());

		oldStructureSource.setStructureStore(structureStore);
		backfillPublicStructureSource.setStructureStore(structureStore);
		adam4EveBackfillStructureSource.setStructureStore(structureStore);
		sirSmashAlotBackfillStructureSource.setStructureStore(structureStore);
		publicStructureSource.setStructureStore(structureStore);
		marketOrdersStructureSource.setStructureStore(structureStore);
		publicContractsStructureSource.setStructureStore(structureStore);
		sovereigntyStructureSource.setStructureStore(structureStore);
	}

	public Completable run() {
		return Completable.concatArray(
				initLogin(),
				initScrapeTime(),
				initMvStore(),
				initMarketStructures(),
				loadPreviousScrape(),
				clearOldStructures(),
				prepareStructureIds()
						.flatMapCompletable(
								id -> Completable.concatArray(
										fetchStructure(id),
										fetchMarket(id),
										Completable.fromAction(() -> progressReporter.increment())),
								false,
								1),
				populateLocations(),
				populateOwners(),
				populateAlliances(),
				buildOutput().flatMapCompletable(this::uploadFiles));
	}

	private Completable initLogin() {
		return getAccessToken().ignoreElement();
	}

	private Completable initScrapeTime() {
		return Completable.fromAction(() -> {
			if (scrapeTime == null) {
				scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
			}
			publicStructureSource.setTimestamp(scrapeTime.toInstant());
			sovereigntyStructureSource.setTimestamp(scrapeTime.toInstant());
			adam4EveBackfillStructureSource.setTimestamp(scrapeTime.toInstant());
		});
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			log.info("Opening MVStore");
			mvStore = mvStoreUtil.createTempStore("public-contracts");
			log.debug("MVStore opened at {}", mvStore.getFileStore().getFileName());
			structureStore.setStore(mvStoreUtil.openJsonMap(mvStore, "structures", Long.class));
			structureStore.setScrapeTime(scrapeTime);
			log.debug("MVStore initialised");
		});
	}

	@SneakyThrows
	private Completable initMarketStructures() {
		return Rx3Util.toSingle(refdataApi.getType(STANDARD_MARKET_HUB_I_TYPE_ID))
				.flatMapCompletable(marketHub -> Completable.fromAction(() -> {
					marketStructureTypeIds = marketHub.getCanFitTypes();
					log.info("Prepared {} market structure type IDs", marketStructureTypeIds.size());
				}));
	}

	private Completable loadPreviousScrape() {
		return Maybe.defer(() -> {
					log.info("Downloading latest structures");
					var baseUrl = Configs.DATA_BASE_URL.getRequired();
					var url = baseUrl + "/" + STRUCTURES.createLatestPath();
					var file = tempFiles.tempFile("structures-latest", ".json").toFile();
					return okHttpHelper.download(url, file, okHttpClient).flatMapMaybe(response -> {
						if (response.code() != 200) {
							log.warn("Failed loading latest structures: HTTP {}, ignoring", response.code());
							return Maybe.empty();
						}
						return Maybe.just(file);
					});
				})
				.flatMapCompletable(file -> {
					var type = objectMapper
							.getTypeFactory()
							.constructMapType(LinkedHashMap.class, String.class, ObjectNode.class);
					Map<String, ObjectNode> map = objectMapper.readValue(file, type);
					log.info("Loaded {} structures from previous scrape", map.size());
					map.forEach((key, value) -> {
						structureStore.put(value);
					});
					structureStore.resetBooleans();
					return Completable.complete();
				})
				.onErrorResumeNext(e -> {
					log.warn("Failed loading latest structures, ignoring", e);
					return Completable.complete();
				});
	}

	private Completable clearOldStructures() {
		return Completable.fromAction(() -> {
			log.info("Clearing old structures");
			var removed = structureStore.removeAllIf(structure -> {
				var latestTimestamp = ALL_TIMESTAMPS.stream()
						.map(prop -> Optional.ofNullable(structure.get(prop)))
						.filter(Optional::isPresent)
						.map(Optional::get)
						.filter(n -> !n.isNull())
						.map(JsonNode::asText)
						.map(Instant::parse)
						.max(Instant::compareTo);
				if (latestTimestamp.isEmpty()) {
					return true;
				}
				var age = Duration.between(latestTimestamp.get(), scrapeTime);
				return age.compareTo(structureTimeout) > 0;
			});
			log.info("Removed {} old structures", removed);
		});
	}

	@NotNull
	private Flowable<Long> prepareStructureIds() {
		AtomicInteger previousIds = new AtomicInteger();
		return Flowable.concatArray(
						Completable.fromAction(() -> previousIds.set(
										structureStore.getAllIds().size()))
								.toFlowable(),
						oldStructureSource.getStructures(),
						// backfillPublicStructureSource.getStructures(),
						// adam4EveBackfillStructureSource.getStructures(),
						// sirSmashAlotBackfillStructureSource.getStructures(),
						publicStructureSource.getStructures(),
						marketOrdersStructureSource.getStructures(),
						publicContractsStructureSource.getStructures()
						// sovereigntyStructureSource.getStructures()
						)
				.distinct()
				.toList()
				.doOnSuccess(ids -> {
					log.info("Prepared {} structures", ids.size());
					var newStructures = ids.size() - previousIds.get();
					log.info("Added {} new structures", newStructures);
					progressReporter = new ProgressReporter(getName(), ids.size(), Duration.ofMinutes(1));
					progressReporter.start();
				})
				.flatMapPublisher(Flowable::fromIterable);
	}

	private Completable fetchStructure(long structureId) {
		return Completable.defer(() -> {
			if (isOrWasSovereigntyStructure(structureId)) {
				return Completable.complete();
			}
			log.trace("Fetching structure {}", structureId);
			var esiUrl = EsiUrl.builder()
					.urlPath(String.format("/universe/structures/%s/", structureId))
					.build();
			return esiHelper.fetch(esiUrl, getAccessToken()).flatMapCompletable(response -> {
				var status = response.code();
				log.debug("Fetched structure {}: {} response", structureId, status);
				if (status == 200) {
					var lastModified =
							structureScrapeHelper.getLastModified(response).orElse(scrapeTime.toInstant());
					var json =
							(ObjectNode) objectMapper.readTree(response.body().bytes());
					structureStore.updateStructure(structureId, json, lastModified);
					structureStore.updateBoolean(structureId, IS_GETTABLE_STRUCTURE, true);
				} else {
					structureStore.updateBoolean(structureId, IS_GETTABLE_STRUCTURE, false);
				}
				return Completable.complete();
			});
		});
	}

	private boolean isOrWasSovereigntyStructure(long structureId) {
		var structure = structureStore.getOrInitStructure(structureId);
		var is = Optional.ofNullable(structure.get(IS_SOVEREIGNTY_STRUCTURE))
				.filter(n -> !n.isNull())
				.map(JsonNode::asBoolean)
				.orElse(false);
		if (is) {
			return true;
		}
		var lastSeen = Optional.ofNullable(structure.get(LAST_SEEN_SOVEREIGNTY_STRUCTURE))
				.filter(n -> !n.isNull());
		return lastSeen.isPresent();
	}

	private Completable fetchMarket(long structureId) {
		return Completable.defer(() -> {
			var structure = structureStore.getOrInitStructure(structureId);
			var typeId = JsonUtil.getNonBlankLongField(structure, "type_id");
			if (typeId.isPresent() && !marketStructureTypeIds.contains(typeId.get())) {
				structureStore.updateBoolean(structureId, IS_MARKET_STRUCTURE, false);
				log.trace("Structure {} of type {} cannot have a market", structureId, typeId.get());
				return Completable.complete();
			}
			log.trace("Fetching market {}", structureId);
			var esiUrl = EsiUrl.builder()
					.urlPath(String.format("/markets/structures/%s/", structureId))
					.build();
			return esiHelper.fetch(esiUrl, getAccessToken()).flatMapCompletable(response -> {
				var status = response.code();
				log.debug("Fetched market {}: {} response", structureId, status);
				if (status == 200) {
					var lastModified =
							structureScrapeHelper.getLastModified(response).orElse(scrapeTime.toInstant());
					structureStore.updateBoolean(structureId, IS_MARKET_STRUCTURE, true);
					structureStore.updateTimestamp(structureId, LAST_SEEN_MARKET_STRUCTURE, lastModified);
				} else {
					structureStore.updateBoolean(structureId, IS_MARKET_STRUCTURE, false);
				}
				return Completable.complete();
			});
		});
	}

	//	@NotNull
	//	private Optional<Long> getTypeId(long structureId) {
	//		return JsonUtil.getNonBlankLongField(structureStore.getOrInitStructure(structureId), "type_id");
	//		return Optional.ofNullable(structureStore.getOrInitStructure(structureId))
	//				.flatMap(n -> Optional.ofNullable(n.get("type_id")))
	//				.filter(n -> !n.isNull())
	//				.map(JsonNode::asLong);
	//	}

	private Completable populateLocations() {
		return Completable.defer(() -> {
			log.info("Populating locations");
			return structureStore.allStructures().flatMapCompletable(pair -> {
				var node = pair.getValue();
				return locationPopulator.populate(pair.getValue()).andThen(Completable.fromAction(() -> {
					structureStore.put(node);
				}));
			});
		});
	}

	private Completable populateOwners() {
		return Completable.defer(() -> {
			log.info("Populating owners");
			return structureStore.allStructures().flatMapCompletable(pair -> {
				var structure = pair.getValue();
				return ownerPopulator.populateOwner(structure).doOnSuccess(s -> {
					structureStore.put(s);
				}).ignoreElement();
			});
		});
	}

	private Single<File> buildOutput() {
		return Single.defer(() -> {
			var file = tempFiles.tempFile("structures", ".json").toFile();
			log.info("Writing output file to {}", file);
			var all = objectMapper.createObjectNode();
			return structureStore
					.allStructures()
					.flatMapCompletable(pair -> Completable.fromAction(() -> {
						all.put(Long.toString(pair.getKey()), pair.getValue());
					}))
					.andThen(Single.fromCallable(() -> {
						objectMapper.writeValue(file, all);
						return file;
					}));
		});
	}

	/**
	 * Uploads the final file to S3.
	 * @return
	 */
	private Completable uploadFiles(@NonNull File outputFile) {
		return Completable.defer(() -> {
			log.info("Uploading files");
			var archiveFile = CompressUtil.compressBzip2(outputFile);
			var latestPath = dataPath.resolve(STRUCTURES.createLatestPath());
			var archivePath = dataPath.resolve(STRUCTURES.createArchivePath(scrapeTime));
			var latestPut = s3Util.putPublicObjectRequest(outputFile.length(), latestPath, latestCacheTime);
			var archivePut = s3Util.putPublicObjectRequest(archiveFile.length(), archivePath, archiveCacheTime);
			log.info(String.format("Uploading latest file to %s", latestPath));
			log.info(String.format("Uploading archive file to %s", archivePath));
			return Completable.mergeArray(
							s3Adapter.putObject(latestPut, outputFile, s3Client).ignoreElement(),
							s3Adapter
									.putObject(archivePut, archiveFile, s3Client)
									.ignoreElement())
					.andThen(Completable.defer(() -> dataIndexHelper.updateIndex(latestPath, archivePath)));
		});
	}

	private Maybe<String> getAccessToken() {
		return esiAuthHelper.getTokenStringForOwnerHash(scrapeOwnerHash).toMaybe();
	}
}
