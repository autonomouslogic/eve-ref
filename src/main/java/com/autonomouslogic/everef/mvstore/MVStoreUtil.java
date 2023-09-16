package com.autonomouslogic.everef.mvstore;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.ObjectDataType;

@Singleton
@Log4j2
public class MVStoreUtil {
	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected JsonNodeDataType jsonNodeDataType;

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
		var builder = new MVStore.Builder()
				.fileName(file.getAbsolutePath())
				.autoCompactFillRate(0)
				.compress()
				.cacheSize(cacheSize);
		var store = builder.open();
		store.setVersionsToKeep(0);
		return store;
	}

	public <T> MVMap<T, JsonNode> openJsonMap(
			@NonNull MVStore mvStore, @NonNull String name, @NonNull Class<T> keyType) {
		return mvStore.openMap(
				name,
				new MVMap.Builder<T, JsonNode>().keyType(new ObjectDataType()).valueType(jsonNodeDataType));
	}
}
