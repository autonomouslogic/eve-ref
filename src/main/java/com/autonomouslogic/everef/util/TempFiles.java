package com.autonomouslogic.everef.util;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;

@Singleton
public class TempFiles {
	@Inject
	public TempFiles() {}

	@SneakyThrows
	public Path tempFile(String prefix, String suffix) {
		var path = Files.createTempFile(prefix + "-", suffix);
		path.toFile().deleteOnExit();
		return path;
	}
}
