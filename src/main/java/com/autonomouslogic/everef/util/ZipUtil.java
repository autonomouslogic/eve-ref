package com.autonomouslogic.everef.util;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;

@Slf4j
public class ZipUtil {
	public static void extractZip(@NonNull File zipFile, @NonNull File outputDir) throws IOException {
		extractZip(new FileInputStream(zipFile), outputDir, false);
	}

	public static void extractZip(@NonNull InputStream fin, @NonNull File outputDir, boolean deleteOnExit)
			throws IOException {
		outputDir.mkdirs();
		if (!outputDir.isDirectory()) {
			throw new IOException(String.format("Failed to create dir: %s", outputDir));
		}
		if (deleteOnExit) {
			outputDir.deleteOnExit();
		}
		try (ArchiveInputStream zin = new ZipArchiveInputStream(fin)) {
			ArchiveEntry entry;
			while ((entry = zin.getNextEntry()) != null) {
				if (!zin.canReadEntryData(entry)) {
					throw new IOException(String.format("Can't read zip entry: %s", entry.getName()));
				}
				File f = new File(outputDir, entry.getName());
				if (!f.toString().startsWith(outputDir.toString())) {
					throw new IllegalStateException(f.toString());
				}
				if (entry.isDirectory()) {
					f.mkdirs();
					if (!f.isDirectory()) {
						throw new IOException("Failed to create directory: " + f);
					}
				} else {
					Files.createParentDirs(f);
					try (OutputStream o = new FileOutputStream(f)) {
						IOUtils.copy(zin, o);
					}
				}
				if (deleteOnExit) {
					f.deleteOnExit();
				}
			}
		}
	}
}
