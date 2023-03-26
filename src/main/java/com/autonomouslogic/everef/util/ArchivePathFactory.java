package com.autonomouslogic.everef.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.NonNull;

/**
 * Builds URLs for archived data.
 */
@Builder
public class ArchivePathFactory {
	public static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
	public static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	public static final ArchivePathFactory MARKET_ORDERS = ArchivePathFactory.builder()
			.folder("market-orders")
			.filename("market-orders")
			.suffix(".v3.csv.bz2")
			.build();

	public static final ArchivePathFactory PUBLIC_CONTRACTS = ArchivePathFactory.builder()
			.folder("public-contracts")
			.filename("public-contracts")
			.suffix(".v2.tar.bz2")
			.build();

	public static final ArchivePathFactory KILLMAILS = ArchivePathFactory.builder()
			.folder("killmails")
			.filename("killmails")
			.historyFolder(false)
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.bz2")
			.build();

	public static final ArchivePathFactory MARKET_HISTORY = ArchivePathFactory.builder()
			.folder("market-history")
			.filename("market-history")
			.historyFolder(false)
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".csv.bz2")
			.build();

	@NonNull
	String folder;

	@NonNull
	String filename;

	@NonNull
	String suffix;

	@lombok.Builder.Default
	boolean historyFolder = true;

	@lombok.Builder.Default
	boolean yearFolder = true;

	@lombok.Builder.Default
	boolean dateFolder = true;

	@lombok.Builder.Default
	DateTimeFormatter fileDateTimeFormatter = TIMESTAMP_PATTERN;

	public String createLatestPath() {
		return join(List.of(folder, filename + "-latest" + suffix));
	}

	public String createArchivePath(LocalDate date) {
		return createArchivePath(date.atStartOfDay(ZoneOffset.UTC));
	}

	public String createArchivePath(Instant time) {
		return createArchivePath(time.atZone(ZoneOffset.UTC));
	}

	public String createArchivePath(ZonedDateTime time) {
		time = time.withZoneSameInstant(ZoneOffset.UTC);
		return join(List.of(
						Optional.of(folder),
						Optional.ofNullable(historyFolder ? "history" : null),
						Optional.ofNullable(yearFolder ? YEAR_PATTERN.format(time) : null),
						Optional.ofNullable(dateFolder ? DATE_PATTERN.format(time) : null),
						Optional.of(filename + "-" + fileDateTimeFormatter.format(time) + suffix))
				.stream()
				.flatMap(Optional::stream));
	}

	private String join(List<String> parts) {
		return join(parts.stream());
	}

	private String join(Stream<String> stream) {
		return stream.collect(Collectors.joining("/"));
	}
}
