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

	private static final Pattern ORDERSET_PATTERN = Pattern.compile(
			"fuzzwork/ordersets/(\\d{4})/(\\d{4}-\\d{2}-\\d{2})/fuzzwork-orderset-(\\d+)-(\\d{4}-\\d{2}-\\d{2})_(\\d{2}-\\d{2}-\\d{2})\\.csv\\.gz");

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
		return STANDARD.createArchivePath(timestamp);
	}

	@Override
	public String createArchivePath(ZonedDateTime archiveTime) {
		return STANDARD.createArchivePath(archiveTime);
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

		var matcher = ORDERSET_PATTERN.matcher(cleanPath);
		if (!matcher.matches()) {
			return null;
		}

		String dateStr = matcher.group(4);
		String timeStr = matcher.group(5);
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

		var matcher = ORDERSET_PATTERN.matcher(cleanPath);
		if (!matcher.matches()) {
			return null;
		}

		try {
			return Long.parseLong(matcher.group(3));
		} catch (Exception e) {
			return null;
		}
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
