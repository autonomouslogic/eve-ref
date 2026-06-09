package com.autonomouslogic.everef.util.archive;

import com.autonomouslogic.everef.config.Configs;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class MerArchivePathFactory implements ArchivePathFactory {
	private static final StandardArchivePathFactory STANDARD_MER = StandardArchivePathFactory.builder()
			.name("mer")
			.folder("ccp/mer")
			.filename("EVEOnline_MER")
			.historyFolder(false)
			.yearFolder(true)
			.dateFolder(false)
			.fileDateTimeFormatter(new DateTimeFormatterBuilder().appendPattern("yyyyMM").toFormatter())
			.dateFormatterSeparator("_")
			.suffix(".zip")
			.build();

	// Regex patterns for different MER formats
	// yyyyMM format (standard): "ccp/mer/2022/EVEOnline_MER_202210.zip"
	private static final Pattern PATTERN_YYYYMM = Pattern.compile(
			"ccp/mer/(\\d{4})/EVEOnline_MER_(\\d{4})(\\d{2}).*\\.zip");

	// 3-letter month: "ccp/mer/2022/EVEOnline_MER_Oct2022*.zip"
	private static final Pattern PATTERN_MMYYYY_3LETTER = Pattern.compile(
			"ccp/mer/(\\d{4})/EVEOnline_MER_([A-Za-z]{3})(\\d{4}).*\\.zip");

	// 4-letter month: "ccp/mer/2016/EVEOnline_MER_July2016.zip"
	private static final Pattern PATTERN_MMMYYYY_4LETTER = Pattern.compile(
			"ccp/mer/(\\d{4})/EVEOnline_MER_([A-Za-z]{4})(\\d{4}).*\\.zip");

	// Old format 2: "ccp/mer/2022/January_2022_MER.zip"
	private static final Pattern PATTERN_OLD_FORMAT_2 = Pattern.compile(
			"ccp/mer/(\\d{4})/([A-Za-z]+)_(\\d{4})_MER\\.zip");

	private static final DateTimeFormatter MONTH_3LETTER = new DateTimeFormatterBuilder()
			.appendPattern("MMM")
			.toFormatter();

	private static final DateTimeFormatter MONTH_4LETTER = new DateTimeFormatterBuilder()
			.appendPattern("MMMM")
			.toFormatter();

	@Override
	public String getName() {
		return STANDARD_MER.getName();
	}

	@Override
	public String getFolder() {
		return STANDARD_MER.getFolder();
	}

	@Override
	public String createLatestPath() {
		return STANDARD_MER.createLatestPath();
	}

	@Override
	public String createArchivePath(LocalDate datestamp) {
		return STANDARD_MER.createArchivePath(datestamp);
	}

	@Override
	public String createArchivePath(Instant timestamp) {
		return STANDARD_MER.createArchivePath(timestamp);
	}

	@Override
	public String createArchivePath(ZonedDateTime archiveTime) {
		return STANDARD_MER.createArchivePath(archiveTime);
	}

	@Override
	public Instant parseArchiveTime(String path) {
		var dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
		path = StringUtils.removeStart(path, dataBaseUrl.getPath());
		path = StringUtils.removeStart(path, "/");

		// Try yyyyMM format (standard)
		var matcher = PATTERN_YYYYMM.matcher(path);
		if (matcher.matches()) {
			int year = Integer.parseInt(matcher.group(2));
			int month = Integer.parseInt(matcher.group(3));
			return LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
		}

		// Try 3-letter month format
		matcher = PATTERN_MMYYYY_3LETTER.matcher(path);
		if (matcher.matches()) {
			String monthStr = matcher.group(2);
			int year = Integer.parseInt(matcher.group(3));
			return parseMonthYear(monthStr, year);
		}

		// Try 4-letter month format
		matcher = PATTERN_MMMYYYY_4LETTER.matcher(path);
		if (matcher.matches()) {
			String monthStr = matcher.group(2);
			int year = Integer.parseInt(matcher.group(3));
			return parseMonthYear(monthStr, year);
		}

		// Try old format 2 (MMMM_yyyy_MER)
		matcher = PATTERN_OLD_FORMAT_2.matcher(path);
		if (matcher.matches()) {
			String monthStr = matcher.group(2);
			int year = Integer.parseInt(matcher.group(3));
			return parseMonthYear(monthStr, year);
		}

		return null;
	}

	private Instant parseMonthYear(String monthStr, int year) {
		try {
			// Try 3-letter month first
			var parsed = MONTH_3LETTER.parse(monthStr);
			int month = parsed.get(ChronoField.MONTH_OF_YEAR);
			return LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
		} catch (Exception e1) {
			try {
				// Try 4-letter month
				var parsed = MONTH_4LETTER.parse(monthStr);
				int month = parsed.get(ChronoField.MONTH_OF_YEAR);
				return LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
			} catch (Exception e2) {
				return null;
			}
		}
	}
}
