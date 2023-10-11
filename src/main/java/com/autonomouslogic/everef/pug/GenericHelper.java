package com.autonomouslogic.everef.pug;

import org.apache.commons.io.FilenameUtils;

public class GenericHelper {
	public String basename(String path) {
		return FilenameUtils.getBaseName(path);
	}

	public String filename(String path) {
		return FilenameUtils.getName(path);
	}
}
