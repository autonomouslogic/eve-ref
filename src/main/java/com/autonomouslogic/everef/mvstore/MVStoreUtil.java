package com.autonomouslogic.everef.mvstore;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

@Singleton
@Log4j2
public class MVStoreUtil {
	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected ObjectMapper objectMapper;

	private final int cacheSize = Configs.MVSTORE_CACHE_SIZE_MB.getRequired();

	@Inject
	protected MVStoreUtil() {}

	/**
	 * Creates and opens a temporary MVStore.
	 * The store will be set to automatically delete when the JVM exits and configured for speed.
	 * @param name
	 * @return
	 */
	@SneakyThrows
	public MVStore createTempStore(String name) {
		log.debug("Creating temporary MVStore '{}' with {} MiB cache", name, cacheSize);
		var file = tempFiles.tempFile(name, ".mvstore").toFile();
		log.debug("MVStore '{}' opened at {}", name, file.getAbsolutePath());
		var builder = new MVStore.Builder().fileName(file.getAbsolutePath()).cacheSize(cacheSize);
		var store = builder.open();
		store.setVersionsToKeep(0);
		return store;
	}

	public <K> Map<K, JsonNode> openJsonMap(@NonNull MVStore mvStore, @NonNull String name, @NonNull Class<K> keyType) {
		MVMap<K, byte[]> map = mvStore.openMap(name);
		return new JsonNodeMap<K>(map, objectMapper);
	}

	public void compact(MVStore mvStore) {
		var file = new File(mvStore.getFileStore().getFileName());
		var size = file.length();
		log.debug(String.format("Compacting %s, current size: %.2f MiB", file, size / 1024.0 / 1024.0));
		mvStore.compactFile(30_000);
		var newSize = file.length();
		var reduction = 1.0 - (double) newSize / (double) size;
		log.debug(String.format(
				"Compacted %s, new size: %.2f MiB - reduction: %.1f%%",
				file, size / 1024.0 / 1024.0, reduction * 100.0));
	}
}
