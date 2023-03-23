package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;

import javax.inject.Singleton;
import java.io.File;

@Module
public class OkHttpModule {
	@Provides
	@Singleton
	public Cache cache() {
		var cacheDir = new File(Configs.HTTP_CACHE_DIR.getRequired());
		if (!cacheDir.exists()) {
			if (!cacheDir.mkdirs()) {
				throw new RuntimeException("Failed creating cache directory: " + cacheDir);
			}
		}
		if (!cacheDir.isDirectory()) {
			throw new RuntimeException("Cache dir is not a directory: " + cacheDir);
		}
		if (!cacheDir.canWrite()) {
			throw new RuntimeException("Cache dir is not writable: " + cacheDir);
		}
		var maxSize = Configs.HTTP_CACHE_SIZE_MB.getRequired();
		if (maxSize < 1) {
			throw new RuntimeException("HTTP cache size must be at least 1 MB");
		}
		return new Cache(cacheDir, maxSize * 1024 * 1024);
	}
}
