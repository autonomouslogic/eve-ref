package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.refdata.RefDataMerger;
import com.autonomouslogic.everef.refdata.esi.EsiLoader;
import com.autonomouslogic.everef.refdata.sde.SdeLoader;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

@Log4j2
public class BuildRefData implements Command {
	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

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

	private MVStore mvStore;
	private StoreSet typeStores;

	@Inject
	protected BuildRefData() {}

	@Override
	public Completable run() {
		return Completable.concatArray(
				initMvStore(),
				Completable.mergeArray(
						downloadLatestSde().flatMapCompletable(sdeLoader::load),
						downloadLatestEsi().flatMapCompletable(esiLoader::load)),
				mergeDatasets(),
				buildOutputFile().flatMapCompletable(this::uploadFile),
				closeMvStore());
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			mvStore = mvStoreUtil.createTempStore("ref-data");
			typeStores = openStoreSet("types");

			sdeLoader.setTypeStore(typeStores.getSdeStore());
			esiLoader.setTypeStore(typeStores.getEsiStore());
		});
	}

	private Completable mergeDatasets() {
		return Completable.defer(() -> {
			return refDataMergerProvider
					.get()
					.setName("types")
					.setStores(typeStores)
					.merge();
		});
	}

	private Completable closeMvStore() {
		return Completable.fromAction(() -> {
			mvStore.close();
		});
	}

	private Single<File> downloadLatestSde() {
		return Single.defer(() -> {
			var url = "https://data.everef.net/ccp/sde/sde-20230315-TRANQUILITY.zip"; // @todo
			var file = tempFiles.tempFile("sde", ".zip").toFile();
			return okHttpHelper.download(url, file, okHttpClient).flatMap(response -> {
				if (response.code() != 200) {
					return Single.error(new RuntimeException("Failed downloading ESI"));
				}
				return Single.just(file);
			});
		});
	}

	private Single<File> downloadLatestEsi() {
		return Single.defer(() -> {
			var url = "https://data.everef.net/esi-scrape/eve-ref-esi-scrape-latest.tar.xz"; // @todo
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
						writeEntries("types", typeStores.getRefStore(), tar);
					}
					log.debug(String.format("Wrote %.0f MiB to %s", (double) file.length() / 1024.0 / 1024.0, file));
					var compressed = CompressUtil.compressXz(file);
					file.delete();
					return compressed;
				})
				.compose(Rx.offloadSingle());
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

	private Completable uploadFile(@NonNull File compressed) {
		return Completable.defer(() -> {
			var dir = new File("/tmp/ref-data");
			dir.mkdirs();
			return CompressUtil.loadArchive(compressed).flatMapCompletable(pair -> {
				var entry = pair.getKey();
				var bytes = pair.getValue();
				var file = new File(dir, entry.getName());
				log.debug("Extracting: " + file);
				var json = objectMapper.readTree(bytes);
				var pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(json);
				FileUtils.writeByteArrayToFile(file, pretty);
				return Completable.complete();
			});
		});
	}

	private StoreSet openStoreSet(String name) {
		return new StoreSet(
				mvStoreUtil.openJsonMap(mvStore, name + "-sde", Long.class),
				mvStoreUtil.openJsonMap(mvStore, name + "-esi", Long.class),
				mvStoreUtil.openJsonMap(mvStore, name + "-ref", Long.class));
	}
}
