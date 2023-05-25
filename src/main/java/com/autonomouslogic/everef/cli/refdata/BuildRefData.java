package com.autonomouslogic.everef.cli.refdata;

import static com.autonomouslogic.everef.util.ArchivePathFactory.ESI;
import static com.autonomouslogic.everef.util.ArchivePathFactory.REFERENCE_DATA;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.refdata.esi.EsiLoader;
import com.autonomouslogic.everef.cli.refdata.sde.SdeLoader;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.S3Util;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.h2.mvstore.MVMap;
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
	protected Provider<RefDataMerger> refDataMergerProvider;

	@Inject
	protected Provider<DataCrawler> dataCrawlerProvider;

	@Setter
	@NonNull
	private ZonedDateTime buildTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	private S3Url dataUrl;
	private URI dataBaseUrl;
	private MVStore mvStore;
	private StoreSet typeStores;
	private StoreSet dogmaAttributesStores;

	@Inject
	protected BuildRefData() {}

	@Inject
	protected void init() {
		dataUrl = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
	}

	@Override
	public Completable run() {
		return Completable.concatArray(
				initMvStore(),
				Completable.mergeArray(
						downloadLatestSde().flatMapCompletable(sdeLoader::load),
						downloadLatestEsi().flatMapCompletable(esiLoader::load)),
				mergeDatasets(),
				buildOutputFile().flatMapCompletable(this::uploadFiles),
				closeMvStore());
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			mvStore = mvStoreUtil.createTempStore("ref-data");
			typeStores = openStoreSet("types");
			dogmaAttributesStores = openStoreSet("dogma-attributes");

			sdeLoader.setTypeStore(typeStores.getSdeStore());
			esiLoader.setTypeStore(typeStores.getEsiStore());
			sdeLoader.setDogmaAttributesStore(dogmaAttributesStores.getSdeStore());
			esiLoader.setDogmaAttributesStore(dogmaAttributesStores.getEsiStore());
		});
	}

	private Completable mergeDatasets() {
		return Completable.mergeArray(
				refDataMergerProvider
						.get()
						.setName("types")
						.setStores(typeStores)
						.merge(),
				refDataMergerProvider
						.get()
						.setName("dogma-attributes")
						.setStores(dogmaAttributesStores)
						.merge());
	}

	private Completable closeMvStore() {
		return Completable.fromAction(() -> {
			mvStore.close();
		});
	}

	private Single<File> downloadLatestSde() {
		return dataCrawlerProvider
				.get()
				.setPrefix("/ccp/sde")
				.crawl()
				.filter(url -> url.toString().endsWith("-TRANQUILITY.zip"))
				.sorted()
				.lastElement()
				.switchIfEmpty(Single.error(new RuntimeException("No SDE found")))
				.flatMap(url -> {
					log.info("Using SDE at: {}", url);
					var file = tempFiles.tempFile("sde", ".zip").toFile();
					return okHttpHelper
							.download(url.toString(), file, okHttpClient)
							.flatMap(response -> {
								if (response.code() != 200) {
									return Single.error(
											new RuntimeException("Failed downloading ESI: " + response.code()));
								}
								return Single.just(file);
							});
				});
	}

	private Single<File> downloadLatestEsi() {
		return Single.defer(() -> {
			var url = dataBaseUrl + "/" + ESI.createLatestPath();
			var file = tempFiles.tempFile("esi", ".tar.xz").toFile();
			return okHttpHelper.download(url, file, okHttpClient).flatMap(response -> {
				if (response.code() != 200) {
					return Single.error(new RuntimeException("Failed downloading ESI"));
				}
				return Single.just(file);
			});
		});
	}

	private Single<File> buildOutputFile() {
		return Single.fromCallable(() -> {
					var file = File.createTempFile("ref-data-", ".tar");
					log.info("Writing ref data to {}", file);
					try (var tar = new TarArchiveOutputStream(new FileOutputStream(file))) {
						writeMeta(tar);
						writeEntries("types", typeStores.getRefStore(), tar);
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
		var meta =
				objectMapper.writeValueAsBytes(objectMapper.createObjectNode().put("build_time", buildTime.toString()));
		var archiveEntry = new TarArchiveEntry("meta.json");
		archiveEntry.setSize(meta.length);
		tar.putArchiveEntry(archiveEntry);
		try (var in = new ByteArrayInputStream(meta)) {
			IOUtils.copy(in, tar);
		}
		tar.closeArchiveEntry();
	}

	@SneakyThrows
	private void writeEntries(String name, MVMap<Long, JsonNode> store, TarArchiveOutputStream tar) {
		var file = tempFiles.tempFile("ref-data" + name, ".json").toFile();
		try (var generator = objectMapper.createGenerator(new FileOutputStream(file))) {
			generator.writeStartObject();
			for (var entry : store.entrySet()) {
				generator.writeFieldName(entry.getKey().toString());
				objectMapper.writeValue(generator, entry.getValue());
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

	private StoreSet openStoreSet(String name) {
		return new StoreSet(
				mvStoreUtil.openJsonMap(mvStore, name + "-sde", Long.class),
				mvStoreUtil.openJsonMap(mvStore, name + "-esi", Long.class),
				mvStoreUtil.openJsonMap(mvStore, name + "-ref", Long.class));
	}
}
