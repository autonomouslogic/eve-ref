package com.autonomouslogic.everef.pug;

import de.neuland.pug4j.template.ClasspathTemplateLoader;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.TemplateLoader;
import java.io.File;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

/**
 * Builds a Pug template loader.
 * If the files are accessible via source code on disk, a {@link FileTemplateLoader} is used.
 * Otherwise, a {@link ClasspathTemplateLoader} is used.
 * This is so files can be changed during development without having to rebuild.
 */
@Singleton
@Log4j2
public class TemplateLoaderFactory {
	private static final String DEV_SRC_DIR = "src/main/resources/pug";
	private static final String BUILD_SRC_DIR = "pug";

	@Inject
	protected TemplateLoaderFactory() {}

	public TemplateLoader create() {
		return tryDir(new File(DEV_SRC_DIR).getAbsoluteFile())
				.or(() -> tryDir(new File(BUILD_SRC_DIR).getAbsoluteFile()))
				.orElseThrow(() -> new IllegalStateException("Could not find Pug templates"));
	}

	private static Optional<FileTemplateLoader> tryDir(File dir) {
		log.debug(String.format("Trying local directory: %s", dir));
		if (dir.isDirectory()) {
			log.debug(String.format("Using local directory: %s", dir));
			return Optional.of(new FileTemplateLoader(dir.getPath()));
		}
		return Optional.empty();
	}
}
