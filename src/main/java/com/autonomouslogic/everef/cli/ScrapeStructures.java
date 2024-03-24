package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.STRUCTURES;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.infrastructure.Success;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.HttpUrl;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	private static final String STRUCTURE_ID = "structure_id";
	private static final String IS_PUBLIC = "is_public";
	private static final String LAST_INFORMATION_UPDATE = "last_information_update";
	private static final String LAST_SEEN_PUBLIC_ID = "last_seen_public_id";

	private static final List<String> ALL_CUSTOM_PROPERTIES =
			List.of(STRUCTURE_ID, IS_PUBLIC, LAST_INFORMATION_UPDATE, LAST_SEEN_PUBLIC_ID);

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

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private final String scrapeOwnerHash = Configs.SCRAPE_CHARACTER_OWNER_HASH.getRequired();
	private S3Url dataPath;
	private HttpUrl dataUrl;
	private String accessToken;

	private MVStore mvStore;
	private Map<Long, JsonNode> store;
	private final Instant timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS);

	@Inject
	protected ScrapeStructures() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		dataUrl = (HttpUrl) urlParser.parse(Configs.DATA_BASE_URL.getRequired());
	}

	public Completable run() {
		return Completable.concatArray(
				initMvStore(),
				initLogin(),
				loadPreviousScrape(),
				Flowable.concatArray(
								fetchPublicStructureIds()
								//							.takeLast(50)
								)
						.flatMapCompletable(this::fetchStructure, false, 1),
				populateLocations(),
				buildOutput().flatMapCompletable(this::uploadFiles));
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			log.info("Opening MVStore");
			mvStore = mvStoreUtil.createTempStore("public-contracts");
			log.debug("MVStore opened at {}", mvStore.getFileStore().getFileName());
			store = mvStoreUtil.openJsonMap(mvStore, "structures", Long.class);
			log.debug("MVStore initialised");
		});
	}

	private Completable initLogin() {
		return Completable.defer(() -> {
			log.debug("Using login for owner hash: {}", scrapeOwnerHash);
			return esiAuthHelper
					.getCharacterLogin(scrapeOwnerHash)
					.switchIfEmpty((Maybe.defer(() -> Maybe.error(new RuntimeException(
							String.format("Login not found for owner hash: %s", scrapeOwnerHash))))))
					.flatMapCompletable(login -> {
						log.debug("Refreshing token");
						return esiAuthHelper
								.refreshAccessToken(login.getRefreshToken())
								.flatMapCompletable(token -> {
									accessToken = token.getAccessToken();
									log.debug("Token refreshed: {}", accessToken);
									return Completable.complete();
								});
					});
		});
	}

	private Completable loadPreviousScrape() {
		return Completable.complete();
	}

	private Flowable<Long> fetchPublicStructureIds() {
		return Flowable.defer(() -> {
					var response = universeApi.getUniverseStructuresWithHttpInfo(
							UniverseApi.DatasourceGetUniverseStructures.tranquility, null, null);
					if (response.getStatusCode() != 200) {
						return Flowable.error(new RuntimeException(
								String.format("Failed to fetch public structure ids: %s", response.getStatusCode())));
					}
					var ids = ((Success<Set<Long>>) response).getData();
					var lastModified = Optional.ofNullable(response.getHeaders().get("last-modified")).stream()
							.flatMap(l -> l.stream())
							.findFirst()
							.map(t -> ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME)
									.toInstant())
							.orElse(timestamp);
					log.debug("Fetched {} public structure ids", ids.size());
					return Flowable.fromIterable(ids)
							.observeOn(Schedulers.computation())
							.doOnNext(id -> {
								var node = (ObjectNode) store.get(id);
								if (node == null) {
									node = objectMapper.createObjectNode();
								}
								node.put(IS_PUBLIC, true);
								node.put(LAST_SEEN_PUBLIC_ID, lastModified.toString());
								store.put(id, node);
							});
				})
				.subscribeOn(Schedulers.io());
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
				var json = (ObjectNode) store.get(structureId);
				json.put(STRUCTURE_ID, structureId);
				if (status == 200) {
					var lastModified = Optional.ofNullable(response.header("last-modified"))
							.map(t -> ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME)
									.toInstant())
							.orElse(timestamp);
					var newJson =
							(ObjectNode) objectMapper.readTree(response.body().bytes());
					for (var prop : ALL_CUSTOM_PROPERTIES) {
						newJson.set(prop, json.get(prop));
					}
					newJson.put(LAST_INFORMATION_UPDATE, lastModified.toString());
					json = newJson;
				}
				store.put(structureId, json);
				return Completable.complete();
			});
		});
	}

	private Completable populateLocations() {
		return Completable.defer(() -> {
			log.info("Populating locations");
			return Flowable.fromIterable(store.keySet()).flatMapCompletable(id -> {
				var node = (ObjectNode) store.get(id);
				return locationPopulator.populate(node).andThen(Completable.fromAction(() -> {
					store.put(id, node);
				}));
			});
		});
	}

	private Single<File> buildOutput() {
		return Single.defer(() -> {
			log.info("Building output file");
			var file = new File("/tmp/structures.json");
			log.info("Writing output file to {}", file);
			var ids = new ArrayList<>(store.keySet());
			ids.sort(Long::compareTo);
			var all = objectMapper.createObjectNode();
			for (var id : ids) {
				all.put(Long.toString(id), store.get(id));
			}
			objectMapper.writeValue(file, all);
			return Single.just(file);
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
