package com.autonomouslogic.everef.util.archive;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public interface ArchivePathFactory {
	String getName();

	String getFolder();

	String createLatestPath();

	Instant parseArchiveTime(String path);

	String createArchivePath(LocalDate datestamp);

	String createArchivePath(Instant timestamp);

	String createArchivePath(ZonedDateTime archiveTime);
}
