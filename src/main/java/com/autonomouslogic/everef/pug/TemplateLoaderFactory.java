package com.autonomouslogic.everef.pug;

import de.neuland.pug4j.template.ClasspathTemplateLoader;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.TemplateLoader;
import java.io.File;
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
	private static final String SRC_DIR = "src/main/resources/pug";
	private static final String RESOURCE_DIR = "/pug";

	@Inject
	protected TemplateLoaderFactory() {}

	public TemplateLoader create() {
		var dir = new File(SRC_DIR).getAbsoluteFile();
		log.debug(String.format("Trying local directory: %s", dir));
		if (dir.isDirectory()) {
			log.debug(String.format("Using local directory: %s", dir));
			return new FileTemplateLoader(dir.getPath());
		}
		log.debug("Local directory not found, using classpath loader");
		return new ClasspathTemplateLoader(RESOURCE_DIR);
	}
}
