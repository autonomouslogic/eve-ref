package com.autonomouslogic.everef.util.archive;

import com.autonomouslogic.everef.config.Configs;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Matches the per-part files for the full EVE Ref deep archive, e.g.
 * "deep-archive/everef-eve-online-deep-archive-00.torrent". These have no date in the filename; the part number
 * is reported as the sequence number.
 */
public class DeepArchiveArchivePathFactory implements ArchivePathFactory {
	private static final String FOLDER = "deep-archive";
	private static final String FILENAME = "everef-eve-online-deep-archive";

	private final String name;
	private final String suffix;
	private final Pattern pattern;

	public DeepArchiveArchivePathFactory(String name, String suffix) {
		this.name = name;
		this.suffix = suffix;
		this.pattern = Pattern.compile("deep-archive/everef-eve-online-deep-archive-(\\d+)" + Pattern.quote(suffix));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFolder() {
		return FOLDER;
	}

	@Override
	public String createLatestPath() {
		return FOLDER + "/" + FILENAME + "-latest" + suffix;
	}

	@Override
	public String createArchivePath(LocalDate datestamp) {
		return createArchivePath(0, datestamp);
	}

	@Override
	public String createArchivePath(Instant timestamp) {
		return createArchivePath(0, timestamp);
	}

	@Override
	public String createArchivePath(ZonedDateTime archiveTime) {
		return createArchivePath(0, archiveTime);
	}

	@Override
	public String createArchivePath(long sequenceNumber, LocalDate datestamp) {
		return formatPath(sequenceNumber);
	}

	@Override
	public String createArchivePath(long sequenceNumber, Instant timestamp) {
		return formatPath(sequenceNumber);
	}

	@Override
	public String createArchivePath(long sequenceNumber, ZonedDateTime archiveTime) {
		return formatPath(sequenceNumber);
	}

	private String formatPath(long sequenceNumber) {
		return FOLDER + "/" + FILENAME + "-" + String.format("%02d", sequenceNumber) + suffix;
	}

	@Override
	public Instant parseArchiveTime(String path) {
		return null;
	}

	@Override
	public Long parseSequenceNumber(String path) {
		var matcher = pattern.matcher(stripBaseUrl(path));
		if (matcher.matches()) {
			return Long.parseLong(matcher.group(1));
		}
		return null;
	}

	private static String stripBaseUrl(String path) {
		var dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
		path = StringUtils.removeStart(path, dataBaseUrl.getPath());
		return StringUtils.removeStart(path, "/");
	}
}
