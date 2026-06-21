package com.autonomouslogic.everef.util.archive;

import com.autonomouslogic.everef.config.Configs;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class FuzzworkOrdersetArchivePathFactory implements ArchivePathFactory {
	private static final StandardArchivePathFactory STANDARD = StandardArchivePathFactory.builder()
			.name("fuzzwork-ordersets")
			.folder("fuzzwork/ordersets")
			.historyFolder(false)
			.filename("fuzzwork-orderset-")
			.suffix(".csv.gz")
			.build();

	// Pattern with sequence number: fuzzwork-orderset-{ID}-YYYY-MM-DD_HH-MM-SS.csv.gz
	private static final Pattern ORDERSET_WITH_SEQUENCE = Pattern.compile(
			"fuzzwork/ordersets/(\\d{4})/(\\d{4}-\\d{2}-\\d{2})/fuzzwork-orderset-(\\d+)-(\\d{4}-\\d{2}-\\d{2})_(\\d{2}-\\d{2}-\\d{2})\\.csv\\.gz");

	// Pattern without sequence number: fuzzwork-orderset-YYYY-MM-DD_HH-MM-SS.csv.gz (used by data index)
	private static final Pattern ORDERSET_WITHOUT_SEQUENCE = Pattern.compile(
			"fuzzwork/ordersets/(\\d{4})/(\\d{4}-\\d{2}-\\d{2})/fuzzwork-orderset-(\\d{4}-\\d{2}-\\d{2})_(\\d{2}-\\d{2}-\\d{2})\\.csv\\.gz");

	// Pattern for backfill/historical data: orderset-{ID}.csv.gz (no timestamp)
	private static final Pattern ORDERSET_BACKFILL = Pattern.compile(
			"fuzzwork/ordersets/backfills/.*/orderset-(\\d+)\\.csv\\.gz");

	@Override
	public String getName() {
		return STANDARD.getName();
	}

	@Override
	public String getFolder() {
		return STANDARD.getFolder();
	}

	@Override
	public String createLatestPath() {
		return STANDARD.createLatestPath();
	}

	@Override
	public String createArchivePath(LocalDate datestamp) {
		return STANDARD.createArchivePath(datestamp);
	}

	@Override
	public String createArchivePath(Instant timestamp) {
		return createArchivePath(timestamp.atZone(ZoneOffset.UTC));
	}

	@Override
	public String createArchivePath(ZonedDateTime archiveTime) {
		// For non-sequence pattern, build path manually to avoid double dash
		archiveTime = archiveTime.withZoneSameInstant(ZoneOffset.UTC);
		return String.format(
				"fuzzwork/ordersets/%s/%s/fuzzwork-orderset-%s.csv.gz",
				ArchivePathFactories.YEAR_PATTERN.format(archiveTime),
				ArchivePathFactories.DATE_PATTERN.format(archiveTime),
				ArchivePathFactories.TIMESTAMP_PATTERN.format(archiveTime));
	}

	@Override
	public String createArchivePath(long sequenceNumber, LocalDate datestamp) {
		return createArchivePath(sequenceNumber, datestamp.atStartOfDay(ZoneOffset.UTC));
	}

	@Override
	public String createArchivePath(long sequenceNumber, Instant timestamp) {
		return createArchivePath(sequenceNumber, timestamp.atZone(ZoneOffset.UTC));
	}

	@Override
	public String createArchivePath(long sequenceNumber, ZonedDateTime archiveTime) {
		archiveTime = archiveTime.withZoneSameInstant(ZoneOffset.UTC);
		return String.format(
				"fuzzwork/ordersets/%s/%s/fuzzwork-orderset-%d-%s.csv.gz",
				ArchivePathFactories.YEAR_PATTERN.format(archiveTime),
				ArchivePathFactories.DATE_PATTERN.format(archiveTime),
				sequenceNumber,
				ArchivePathFactories.TIMESTAMP_PATTERN.format(archiveTime));
	}

	@Override
	public Instant parseArchiveTime(String path) {
		String cleanPath = stripBaseUrl(path);

		// Try pattern with sequence number first
		var matcher = ORDERSET_WITH_SEQUENCE.matcher(cleanPath);
		if (matcher.matches()) {
			String dateStr = matcher.group(4);
			String timeStr = matcher.group(5);
			return parseTimestamp(dateStr, timeStr);
		}

		// Try pattern without sequence number (data index pattern)
		matcher = ORDERSET_WITHOUT_SEQUENCE.matcher(cleanPath);
		if (matcher.matches()) {
			String dateStr = matcher.group(3);
			String timeStr = matcher.group(4);
			return parseTimestamp(dateStr, timeStr);
		}

		return null;
	}

	private static Instant parseTimestamp(String dateStr, String timeStr) {
		String timestampStr = dateStr + "_" + timeStr;
		try {
			var formatter = ArchivePathFactories.TIMESTAMP_PATTERN.withZone(ZoneOffset.UTC);
			var parsed = formatter.parse(timestampStr);
			return Instant.from(parsed);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Long parseSequenceNumber(String path) {
		String cleanPath = stripBaseUrl(path);

		// Try pattern with sequence number (main archive)
		var matcher = ORDERSET_WITH_SEQUENCE.matcher(cleanPath);
		if (matcher.matches()) {
			try {
				return Long.parseLong(matcher.group(3));
			} catch (Exception e) {
				return null;
			}
		}

		// Try backfill pattern
		matcher = ORDERSET_BACKFILL.matcher(cleanPath);
		if (matcher.matches()) {
			try {
				return Long.parseLong(matcher.group(1));
			} catch (Exception e) {
				return null;
			}
		}

		// Pattern without sequence number returns null
		return null;
	}

	private static String stripBaseUrl(String path) {
		try {
			var dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
			path = StringUtils.removeStart(path, dataBaseUrl.getPath());
		} catch (Exception e) {
			// DATA_BASE_URL not configured, use path as-is
		}
		path = StringUtils.removeStart(path, "/");
		return path;
	}
}
