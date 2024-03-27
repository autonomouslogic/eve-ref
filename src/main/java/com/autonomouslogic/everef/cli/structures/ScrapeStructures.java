package com.autonomouslogic.everef.cli.structures;

import static com.autonomouslogic.everef.util.ArchivePathFactory.STRUCTURES;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.HttpUrl;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.h2.mvstore.MVStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Scrapes structure information.
 */
@Log4j2
public class ScrapeStructures implements Command {
	public static final String STRUCTURE_ID = "structure_id";
	public static final String LAST_STRUCTURE_GET = "last_structure_get";
	public static final String IS_PUBLIC_STRUCTURE = "is_public_structure";
	public static final String LAST_SEEN_PUBLIC_STRUCTURE = "last_seen_public_structure";

	public static final List<String> ALL_CUSTOM_PROPERTIES =
			List.of(STRUCTURE_ID, IS_PUBLIC_STRUCTURE, LAST_STRUCTURE_GET, LAST_SEEN_PUBLIC_STRUCTURE);

	public static final List<String> ALL_BOOLEANS = List.of(IS_PUBLIC_STRUCTURE);

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
	protected StructureScrapeHelper structureScrapeHelper;

	@Inject
	protected StructureStore structureStore;

	@Inject
	protected OldStructureSource oldStructureSource;

	@Inject
	protected PublicStructureSource publicStructureSource;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private final String scrapeOwnerHash = Configs.SCRAPE_CHARACTER_OWNER_HASH.getRequired();
	private S3Url dataPath;
	private HttpUrl dataUrl;
	private String accessToken;
	private MVStore mvStore;
	private Instant timestamp;

	@Inject
	protected ScrapeStructures() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		dataUrl = (HttpUrl) urlParser.parse(Configs.DATA_BASE_URL.getRequired());
		timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		publicStructureSource.setTimestamp(timestamp);
	}

	public Completable run() {
		return Completable.concatArray(
				initMvStore(),
				Completable.defer(() -> Completable.concatArray(
						initLogin(),
						loadPreviousScrape(),
						Flowable.concatArray(
										oldStructureSource.getStructures(structureStore),
										publicStructureSource.getStructures(structureStore),
										fetchMarketStructureIds(),
										fetchContractStructureIds(),
										fetchSovereigntyStructureIds())
								.flatMapCompletable(this::fetchStructure, false, 1),
						clearOldStructures(),
						populateLocations(),
						buildOutput().flatMapCompletable(this::uploadFiles))));
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			log.info("Opening MVStore");
			mvStore = mvStoreUtil.createTempStore("public-contracts");
			log.debug("MVStore opened at {}", mvStore.getFileStore().getFileName());
			structureStore.setStore(mvStoreUtil.openJsonMap(mvStore, "structures", Long.class));
			log.debug("MVStore initialised");
		});
	}

	private Completable initLogin() {
		return Completable.defer(() -> {
			log.debug("Using login for owner hash: {}", scrapeOwnerHash);
			return esiAuthHelper
					.getTokenForOwnerHash(scrapeOwnerHash)
					.switchIfEmpty((Maybe.defer(() -> Maybe.error(new RuntimeException(
							String.format("Login not found for owner hash: %s", scrapeOwnerHash))))))
					.flatMapCompletable(token -> {
						accessToken = token.getAccessToken();
						log.debug("Token refreshed");
						return Completable.complete();
					});
		});
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
					oldStructureSource.setPreviousScrape(map);
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

	private Flowable<Long> fetchMarketStructureIds() {
		return Flowable.empty(); // @todo
	}

	private Flowable<Long> fetchContractStructureIds() {
		return Flowable.empty(); // @todo
	}

	private Flowable<Long> fetchSovereigntyStructureIds() {
		return Flowable.empty(); // @todo
	}

	private Completable fetchStructure(long structureId) {
		return Completable.defer(() -> {
			log.trace("Fetching structure {}", structureId);
			var esiUrl = EsiUrl.builder()
					.urlPath(String.format("/universe/structures/%s/", structureId))
					.build();
			return esiHelper.fetch(esiUrl, accessToken).flatMapCompletable(response -> {
				var status = response.code();
				log.debug("Fetched structure {}: {} response", structureId, status);
				if (status == 200) {
					var lastModified =
							structureScrapeHelper.getLastModified(response).orElse(timestamp);
					var newJson =
							(ObjectNode) objectMapper.readTree(response.body().bytes());
					structureStore.updateStructure(structureId, newJson, lastModified);
				}
				return Completable.complete();
			});
		});
	}

	private Completable clearOldStructures() {
		// @todo remove any structure with a timestamp earlier than 30 days ago.
		return Completable.complete();
	}

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

	private Single<File> buildOutput() {
		return Single.defer(() -> {
			var file = new File("/tmp/structures.json");
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
			var archivePath = dataPath.resolve(STRUCTURES.createArchivePath(Instant.now()));
			var latestPut = s3Util.putPublicObjectRequest(outputFile.length(), latestPath, latestCacheTime);
			var archivePut = s3Util.putPublicObjectRequest(archiveFile.length(), archivePath, archiveCacheTime);
			log.info(String.format("Uploading latest file to %s", latestPath));
			log.info(String.format("Uploading archive file to %s", archivePath));
			return Completable.mergeArray(
							s3Adapter.putObject(latestPut, outputFile, s3Client).ignoreElement()
							//							s3Adapter
							//									.putObject(archivePut, archiveFile, s3Client)
							//									.ignoreElement()
							)
					.andThen(Completable.defer(() -> dataIndexHelper.updateIndex(
							latestPath
							//						archivePath
							)));
		});
	}
}
