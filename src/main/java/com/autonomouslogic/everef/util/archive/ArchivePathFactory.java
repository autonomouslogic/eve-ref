package com.autonomouslogic.everef.util.archive;

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
	String name;

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
	DateTimeFormatter fileDateTimeFormatter = ArchivePathFactories.TIMESTAMP_PATTERN;

	@lombok.Builder.Default
	String dateFormatterSeparator = "-";

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
			if (parsed.isSupported(ChronoField.YEAR)
					&& parsed.isSupported(ChronoField.MONTH_OF_YEAR)
					&& !parsed.isSupported(ChronoField.DAY_OF_MONTH)) {
				return LocalDate.of(parsed.get(ChronoField.YEAR), parsed.get(ChronoField.MONTH_OF_YEAR), 1)
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
			builder.appendLiteral("/").append(ArchivePathFactories.YEAR_PATTERN);
		}
		if (dateFolder) {
			builder.appendLiteral("/").append(ArchivePathFactories.DATE_PATTERN);
		}
		builder.appendLiteral("/").appendLiteral(filename);
		if (fileDateTimeFormatter != null) {
			builder.appendLiteral(dateFormatterSeparator).append(fileDateTimeFormatter);
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

	@Value
	public static class ArchiveMatch {
		String type;
		Optional<Instant> date;
	}
}
