package com.autonomouslogic.everef.util;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some local files are included in the resources when a jar is built.
 * This class provides access to said files, by first checking the local file and then the resources.
 *
 * Due to a limitation in jade4j, these files have to be accessible on disk. As such, Jade templates are packaged
 * in a zip file, which is included in the jar. When template files are requested, this zip file is extracted to disk
 * and the files in this directory are supplied.
 */
@Singleton
public class LocalFileHelper {
	private static final Logger log = LoggerFactory.getLogger(LocalFileHelper.class);

	// These values are duplicated in build.gradle for Gradle config.
	public static final File CLIENT_BUILD_DIR = new File("./build/client");
	public static final File CLIENT_JADE_DIR = new File("./src/main/jade");

	private File tmpDir;
	private File templateDir;

	@Inject
	protected LocalFileHelper() {}

	/**
	 * Loads the contents of a file, either from supplied local path, if it exists, or from the bundled resources.
	 * @param localBasePath
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public InputStream getFile(File localBasePath, String file) throws FileNotFoundException, IOException {
		// Check local file.
		File local = new File(localBasePath, file);
		if (local.exists()) {
			// log.info(String.format("Returning local file \"%s\"", local));
			return new FileInputStream(local);
		}
		// Check resources.
		InputStream in = getClass().getResourceAsStream("/" + file);
		if (in != null) {
			// log.info(String.format("Found handler file \"%s\"", file));
			return in;
		}
		throw new FileNotFoundException(String.format("Failed loading file from \"%s\" / \"%s\"", localBasePath, file));
	}

	public InputStream getFile(String file) throws FileNotFoundException, IOException {
		return getFile(new File("."), file);
	}

	/**
	 * Loads a Jade template either from the local development directory, or from the bundled zip file.
	 * @param name
	 * @return
	 */
	public File getTemplate(String name) throws FileNotFoundException, IOException {
		// Resolve directory.
		if (templateDir == null) {
			// Check local dir.
			if (CLIENT_JADE_DIR.exists()) {
				templateDir = CLIENT_JADE_DIR;
			} else {
				templateDir = extractTemplates();
			}
		}
		// Resolve template file.
		File file = new File(templateDir, name);
		if (!file.exists()) {
			throw new FileNotFoundException(file.toString());
		}
		return file;
	}

	/**
	 * Returns the directory where the templates are extracted.
	 * Also extracts the templates if they aren't already.
	 */
	private File extractTemplates() throws FileNotFoundException, IOException {
		File dir = Files.createTempDir();
		dir.deleteOnExit();
		//		InputStream resourceIn = new FileInputStream("./build/tmp/templates.archive");
		InputStream resourceIn = getClass().getResourceAsStream("/templates.archive");
		if (resourceIn == null) {
			throw new FileNotFoundException("Resource templates.archive");
		}
		log.info(String.format("Extracting templates.archive from resources to %s", dir.toPath()));
		ZipUtil.extractZip(resourceIn, dir, true);
		return dir;
	}

	private File getTmpDir() {
		if (tmpDir == null) {
			tmpDir = Files.createTempDir();
			tmpDir.deleteOnExit();
		}
		return tmpDir;
	}
}
