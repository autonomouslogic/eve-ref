package com.autonomouslogic.everef.cli.refdata;

import static com.autonomouslogic.everef.util.ArchivePathFactory.REFERENCE_DATA;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.refdata.esi.EsiLoader;
import com.autonomouslogic.everef.cli.refdata.hoboleaks.HoboleaksLoader;
import com.autonomouslogic.everef.cli.refdata.post.BlueprintDecorator;
import com.autonomouslogic.everef.cli.refdata.post.CanFitDecorator;
import com.autonomouslogic.everef.cli.refdata.post.GroupsDecorator;
import com.autonomouslogic.everef.cli.refdata.post.MarketGroupsDecorator;
import com.autonomouslogic.everef.cli.refdata.post.MissingDogmaUnitsDecorator;
import com.autonomouslogic.everef.cli.refdata.post.MutaplasmidDecorator;
import com.autonomouslogic.everef.cli.refdata.post.OreVariationsDecorator;
import com.autonomouslogic.everef.cli.refdata.post.PostDecorator;
import com.autonomouslogic.everef.cli.refdata.post.SkillDecorator;
import com.autonomouslogic.everef.cli.refdata.post.TypesDecorator;
import com.autonomouslogic.everef.cli.refdata.post.VariationsDecorator;
import com.autonomouslogic.everef.cli.refdata.sde.SdeLoader;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.refdata.RefDataConfig;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.refdata.RefDataMeta;
import com.autonomouslogic.everef.refdata.RefDataMetaFileInfo;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.HashUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.h2.mvstore.MVStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Log4j2
public class BuildRefData implements Command {
	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected S3Util s3Util;

	@Inject
	@Named("data")
	protected S3AsyncClient s3Client;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected SdeLoader sdeLoader;

	@Inject
	protected EsiLoader esiLoader;

	@Inject
	protected HoboleaksLoader hoboleaksLoader;

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected DataUtil dataUtil;

	@Inject
	protected DataIndexHelper dataIndexHelper;

	@Inject
	protected Provider<RefDataMerger> refDataMergerProvider;

	@Inject
	protected SkillDecorator skillDecorator;

	@Inject
	protected MutaplasmidDecorator mutaplasmidDecorator;

	@Inject
	protected VariationsDecorator variationsDecorator;

	@Inject
	protected BlueprintDecorator blueprintDecorator;

	@Inject
	protected GroupsDecorator groupsDecorator;

	@Inject
	protected TypesDecorator typesDecorator;

	@Inject
	protected MarketGroupsDecorator marketGroupsDecorator;

	@Inject
	protected OreVariationsDecorator oreVariationsDecorator;

	@Inject
	protected MissingDogmaUnitsDecorator missingDogmaUnitsDecorator;

	@Inject
	protected CanFitDecorator canFitDecorator;

	@Setter
	@NonNull
	private ZonedDateTime buildTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

	@Setter
	private File latestRefDataFile;

	private RefDataMeta latestRefDataMeta;

	@Setter
	private File sdeFile;

	@Setter
	private File esiFile;

	@Setter
	private File hoboleaksFile;

	private RefDataMeta currentRefDataMeta;

	@Setter
	private boolean stopAtUpload = false;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	@Getter
	private StoreHandler storeHandler;

	private List<PostDecorator> allDecorators;

	private S3Url dataUrl;
	private MVStore mvStore;

	@Inject
	protected BuildRefData() {}

	@Inject
	protected void init() {
		dataUrl = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		allDecorators = List.of(
				skillDecorator,
				mutaplasmidDecorator,
				variationsDecorator,
				blueprintDecorator,
				groupsDecorator,
				typesDecorator,
				marketGroupsDecorator,
				oreVariationsDecorator,
				missingDogmaUnitsDecorator,
				canFitDecorator);
	}

	@Override
	public Completable run() {
		return Completable.concatArray(initMvStore(), latestFiles(), checkAndProcess());
	}

	private Completable latestFiles() {
		return Completable.mergeArray(
						latestRefDataMeta().ignoreElement(),
						latestSde().ignoreElement(),
						latestEsi().ignoreElement(),
						latestHoboleaks().ignoreElement())
				.andThen(Completable.fromAction(this::generateRefDataMeta));
	}

	@SneakyThrows
	private void generateRefDataMeta() {
		currentRefDataMeta = RefDataMeta.builder()
				.buildTime(buildTime.toInstant())
				.sde(RefDataMetaFileInfo.builder()
						.sha256(HashUtil.sha256Hex(sdeFile))
						.build())
				.esi(RefDataMetaFileInfo.builder()
						.sha256(HashUtil.sha256Hex(esiFile))
						.build())
				.hoboleaks(RefDataMetaFileInfo.builder()
						.sha256(HashUtil.sha256Hex(hoboleaksFile))
						.build())
				.build();
	}

	private Completable checkAndProcess() {
		return Completable.defer(() -> {
			if (!processingNeeded()) {
				log.info("No update needed");
				return Completable.complete();
			}
			return processData();
		});
	}

	private boolean processingNeeded() {
		if (Configs.FORCE_REF_DATA.getRequired()) {
			return true;
		}
		return !currentRefDataMeta.getSde().equals(latestRefDataMeta.getSde())
				|| !currentRefDataMeta.getEsi().equals(latestRefDataMeta.getEsi())
				|| !currentRefDataMeta.getHoboleaks().equals(latestRefDataMeta.getHoboleaks());
	}

	private Completable processData() {
		return Completable.concatArray(
				Completable.mergeArray(
						sdeLoader.load(sdeFile), esiLoader.load(esiFile), hoboleaksLoader.load(hoboleaksFile)),
				mergeDatasets(),
				postDatasets(),
				Completable.defer(() -> {
					if (stopAtUpload) {
						return Completable.complete();
					}
					return Completable.concatArray(
							buildOutputFile().flatMapCompletable(this::uploadFiles), closeMvStore());
				}));
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			mvStore = mvStoreUtil.createTempStore("ref-data");
			storeHandler = new StoreHandler(mvStoreUtil, mvStore);
			sdeLoader.setStoreHandler(storeHandler);
			esiLoader.setStoreHandler(storeHandler);
			hoboleaksLoader.setStoreHandler(storeHandler);

			for (var decorator : allDecorators) {
				decorator.setStoreHandler(storeHandler);
			}
		});
	}

	private Completable mergeDatasets() {
		return Completable.defer(() -> Completable.merge(refDataUtil.loadReferenceDataConfig().stream()
						.map(config -> refDataMergerProvider
								.get()
								.setName(config.getId())
								.setOutputStoreName(config.getOutputStore())
								.setStoreHandler(storeHandler)
								.merge())
						.toList()))
				.andThen(Completable.fromAction(() -> log.debug("Finished merging datasets")));
	}

	private Completable postDatasets() {
		return Completable.concat(
				allDecorators.stream().map(this::runPostDecorator).toList());
	}

	private Completable runPostDecorator(PostDecorator decorator) {
		return Completable.defer(() -> {
			var name = decorator.getClass().getSimpleName();
			log.info("Starting post {}", name);
			var start = Instant.now();
			return decorator.create().andThen(Completable.fromAction(() -> {
				var duration = Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.MILLIS);
				log.debug("Finished post {} in {}", name, duration);
			}));
		});
	}

	public Completable closeMvStore() {
		return Completable.fromAction(() -> {
			mvStore.close();
		});
	}

	private Single<File> buildOutputFile() {
		return Single.fromCallable(() -> {
					var file = File.createTempFile("ref-data-", ".tar");
					log.info("Writing ref data to {}", file);
					try (var tar = new TarArchiveOutputStream(new FileOutputStream(file))) {
						writeMeta(tar);
						for (RefDataConfig config : refDataUtil.loadReferenceDataConfig()) {
							writeEntries(config.getOutputFile(), storeHandler.getRefStore(config.getId()), tar);
						}
					}
					log.debug(String.format("Wrote %.0f MiB to %s", file.length() / 1024.0 / 1024.0, file));
					var compressed = CompressUtil.compressXz(file);
					compressed.deleteOnExit();
					return compressed;
				})
				.compose(Rx.offloadSingle());
	}

	@SneakyThrows
	private void writeMeta(TarArchiveOutputStream tar) {
		var meta = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(currentRefDataMeta);
		var archiveEntry = new TarArchiveEntry("meta.json");
		archiveEntry.setSize(meta.length);
		tar.putArchiveEntry(archiveEntry);
		try (var in = new ByteArrayInputStream(meta)) {
			IOUtils.copy(in, tar);
		}
		tar.closeArchiveEntry();
	}

	@SneakyThrows
	private void writeEntries(String name, Map<Long, JsonNode> store, TarArchiveOutputStream tar) {
		var file = tempFiles.tempFile("ref-data" + name, ".json").toFile();
		var printer = objectMapper.writerWithDefaultPrettyPrinter();
		try (var generator = printer.createGenerator(new FileOutputStream(file))) {
			generator.writeStartObject();
			for (var entry : store.entrySet()) {
				generator.writeFieldName(entry.getKey().toString());
				printer.writeValue(generator, entry.getValue());
			}
			generator.writeEndObject();
		}
		var archiveEntry = new TarArchiveEntry(name + ".json");
		archiveEntry.setSize(file.length());
		tar.putArchiveEntry(archiveEntry);
		try (var in = new FileInputStream(file)) {
			IOUtils.copy(in, tar);
		}
		tar.closeArchiveEntry();
		file.delete();
	}

	/**
	 * Uploads the final file to S3.
	 * @return
	 */
	private Completable uploadFiles(File outputFile) {
		return Completable.defer(() -> {
			var latestPath = dataUrl.resolve(REFERENCE_DATA.createLatestPath());
			var archivePath = dataUrl.resolve(REFERENCE_DATA.createArchivePath(buildTime));
			var latestPut = s3Util.putPublicObjectRequest(outputFile.length(), latestPath, latestCacheTime);
			var archivePut = s3Util.putPublicObjectRequest(outputFile.length(), archivePath, archiveCacheTime);
			log.info(String.format("Uploading latest file to %s", latestPath));
			log.info(String.format("Uploading archive file to %s", archivePath));
			return Completable.mergeArray(
							s3Adapter.putObject(latestPut, outputFile, s3Client).ignoreElement(),
							s3Adapter
									.putObject(archivePut, outputFile, s3Client)
									.ignoreElement())
					.andThen(Completable.defer(() -> dataIndexHelper.updateIndex(latestPath, archivePath)));
		});
	}

	private Single<File> latestEsi() {
		return esiFile != null
				? Single.just(esiFile)
				: dataUtil.downloadLatestEsi().doOnSuccess(file -> {
					esiFile = file;
				});
	}

	private Single<File> latestSde() {
		return sdeFile != null
				? Single.just(sdeFile)
				: dataUtil.downloadLatestSde().doOnSuccess(file -> {
					sdeFile = file;
				});
	}

	private Single<File> latestHoboleaks() {
		return hoboleaksFile != null
				? Single.just(hoboleaksFile)
				: dataUtil.downloadLatestHoboleaks().doOnSuccess(file -> {
					hoboleaksFile = file;
				});
	}

	private Single<File> latestRefData() {
		return latestRefDataFile != null
				? Single.just(latestRefDataFile)
				: refDataUtil.downloadLatestReferenceData().doOnSuccess(file -> {
					latestRefDataFile = file;
				});
	}

	private Single<RefDataMeta> latestRefDataMeta() {
		return latestRefData().flatMap(file -> CompressUtil.loadArchive(file)
				.filter(e -> e.getKey().getName().equals("meta.json"))
				.map(e -> objectMapper.readValue(e.getValue(), RefDataMeta.class))
				.first(RefDataMeta.builder().build())
				.doOnSuccess(meta -> latestRefDataMeta = meta));
	}
}
