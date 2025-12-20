package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.config.Configs;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

/**
 * Builds URLs for archived data.
 */
@Value
@Builder(toBuilder = true)
public class ArchivePathFactory {
	public static final DateTimeFormatter YEAR_PATTERN = DateTimeFormatter.ofPattern("yyyy");
	public static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	public static final DateTimeFormatter COMPACT_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

	public static final ArchivePathFactory REFERENCE_DATA = ArchivePathFactory.builder()
			.folder("reference-data")
			.filename("reference-data")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build();

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

	public static final ArchivePathFactory FREELANCE_JOBS = ArchivePathFactory.builder()
			.folder("freelance-jobs")
			.filename("freelance-jobs")
			.suffix(".json.bz2")
			.latestSuffix(".json")
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

	public static final ArchivePathFactory SDE = ArchivePathFactory.builder()
			.folder("ccp/sde")
			.filename("sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(COMPACT_DATE_PATTERN)
			.suffix("-TRANQUILITY.zip")
			.build();

	public static final ArchivePathFactory SDE_V2_JSONL = ArchivePathFactory.builder()
			.folder("ccp/sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.filename("eve-online-static-data")
			.fileDateTimeFormatter(null)
			.suffix("-jsonl.zip")
			.build();

	public static final ArchivePathFactory SDE_V2_YAML = ArchivePathFactory.builder()
			.folder("ccp/sde")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.filename("eve-online-static-data")
			.fileDateTimeFormatter(null)
			.suffix("-yaml.zip")
			.build();

	public static final ArchivePathFactory ESI = ArchivePathFactory.builder()
			.folder("esi-scrape")
			.filename("eve-ref-esi-scrape")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build();

	public static final ArchivePathFactory HOBOLEAKS = ArchivePathFactory.builder()
			.folder("hoboleaks-sde")
			.filename("hoboleaks-sde")
			.dateFolder(false)
			.fileDateTimeFormatter(DATE_PATTERN)
			.suffix(".tar.xz")
			.build();

	public static final ArchivePathFactory FUZZWORK_ORDERSET = ArchivePathFactory.builder()
			.folder("fuzzwork/ordersets")
			.historyFolder(false)
			.filename("fuzzwork-orderset-")
			.suffix(".csv.gz")
			.build();

	public static final ArchivePathFactory STRUCTURES = ArchivePathFactory.builder()
			.folder("structures")
			.filename("structures")
			.suffix(".v2.json.bz2")
			.latestSuffix(".v2.json")
			.build();

	@NonNull
	String folder;

	@NonNull
	String filename;

	@NonNull
	String suffix;

	String latestSuffix;

	@lombok.Builder.Default
	boolean historyFolder = true;

	@lombok.Builder.Default
	boolean yearFolder = true;

	@lombok.Builder.Default
	boolean dateFolder = true;

	@lombok.Builder.Default
	DateTimeFormatter fileDateTimeFormatter = TIMESTAMP_PATTERN;

	public String createLatestPath() {
		return join(List.of(
				folder, filename + "-latest" + Optional.ofNullable(latestSuffix).orElse(suffix)));
	}

	public String createArchivePath(LocalDate date) {
		return createArchivePath(date.atStartOfDay(ZoneOffset.UTC));
	}

	public String createArchivePath(Instant time) {
		return createArchivePath(time.atZone(ZoneOffset.UTC));
	}

	public String createArchivePath(ZonedDateTime time) {
		time = time.withZoneSameInstant(ZoneOffset.UTC);
		return createFormatter().format(time);
	}

	public Instant parseArchiveTime(String path) {
		var dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
		path = StringUtils.removeStart(path, dataBaseUrl.getPath());
		path = StringUtils.removeStart(path, "/");
		try {
			var parsed = createFormatter().parse(path);
			if (parsed.isSupported(ChronoField.HOUR_OF_DAY)) {
				return Instant.from(parsed);
			}
			if (parsed.isSupported(ChronoField.YEAR)
					&& !parsed.isSupported(ChronoField.MONTH_OF_YEAR)
					&& !parsed.isSupported(ChronoField.DAY_OF_MONTH)) {
				return LocalDate.of(parsed.get(ChronoField.YEAR), 1, 1)
						.atStartOfDay(ZoneOffset.UTC)
						.toInstant();
			}
			return LocalDate.from(parsed).atStartOfDay(ZoneOffset.UTC).toInstant();
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private DateTimeFormatter createFormatter() {
		var builder = new DateTimeFormatterBuilder();
		builder.appendLiteral(folder);
		if (historyFolder) {
			builder.appendLiteral("/history");
		}
		if (yearFolder) {
			builder.appendLiteral("/").append(YEAR_PATTERN);
		}
		if (dateFolder) {
			builder.appendLiteral("/").append(DATE_PATTERN);
		}
		builder.appendLiteral("/").appendLiteral(filename);
		if (fileDateTimeFormatter != null) {
			builder.appendLiteral("-").append(fileDateTimeFormatter);
		}
		builder.appendLiteral(suffix);
		return builder.toFormatter().withZone(ZoneOffset.UTC);
	}

	private String join(List<String> parts) {
		return join(parts.stream());
	}

	private String join(Stream<String> stream) {
		return stream.collect(Collectors.joining("/"));
	}
}
