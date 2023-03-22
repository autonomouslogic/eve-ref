package com.autonomouslogic.everef.util;

import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class TempFiles {
	@Inject
	protected TempFiles() {

	}

	@SneakyThrows
	public Path tempFile(String prefix, String suffix) {
		var path = Files.createTempFile(prefix + "-", suffix);
		path.toFile().deleteOnExit();
		return path;
	}
}
