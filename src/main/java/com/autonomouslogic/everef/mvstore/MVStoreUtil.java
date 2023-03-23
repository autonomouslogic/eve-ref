package com.autonomouslogic.everef.mvstore;

import com.autonomouslogic.everef.util.TempFiles;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVStore;

@Singleton
@Log4j2
public class MVStoreUtil {
	@Inject
	protected TempFiles tempFiles;

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
		log.debug("Creating temporary MVStore");
		var file = tempFiles.tempFile(name, ".mvstore").toFile();
		log.debug("MVStore opened at " + file.getAbsolutePath());
		var builder = new MVStore.Builder()
				.fileName(file.getAbsolutePath())
				.autoCompactFillRate(0)
				.cacheSize(512);
		var store = builder.open();
		store.setVersionsToKeep(0);
		return store;
	}
}
