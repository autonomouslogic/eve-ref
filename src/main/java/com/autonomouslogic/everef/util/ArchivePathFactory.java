package com.autonomouslogic.everef.util;

import com.google.common.base.Strings;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.NonNull;
import lombok.Setter;

/**
 * Builds URLs for archived data.
 */
@Setter
public class ArchivePathFactory {
	private static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
	private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	@NonNull
	private String folder;

	@NonNull
	private String filename;

	@NonNull
	private String suffix;

	private boolean yearFolder = false;
	private boolean dateFolder = false;

	public String createLatestPath() {
		validate();
		return join(List.of(folder, filename + "-latest" + suffix));
	}

	public String createArchivePath(Instant time) {
		validate();
	}

	private void validate() {
		if (Strings.isNullOrEmpty(folder)) {
			throw new IllegalArgumentException("Folder must be set");
		}
		if (Strings.isNullOrEmpty(filename)) {
			throw new IllegalArgumentException("Filename must be set");
		}
		if (Strings.isNullOrEmpty(suffix)) {
			throw new IllegalArgumentException("Suffix must be set");
		}
	}

	private String join(List<String> parts) {
		return String.join("/", parts);
	}

	public static ArchivePathFactory marketOrders() {
		return new ArchivePathFactory()
				.setFolder("market-orders")
				.setFilename("market-orders")
				.setYearFolder(true)
				.setDateFolder(true)
				.setSuffix(".v3.csv.bz2");
	}
}
