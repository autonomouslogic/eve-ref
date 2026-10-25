package com.autonomouslogic.everef.util.archive;

import com.autonomouslogic.everef.config.Configs;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Wraps a {@link StandardArchivePathFactory} for the dateless SDE v2 JSONL/YAML files, adding support for the
 * build-numbered variant used since the file naming changed, e.g.
 * "ccp/sde/2026/eve-online-static-data-3167101-jsonl.zip". There's no date anywhere in the filename for this
 * variant (the year folder is just where the build happens to land, not when it was built), so only the build
 * number is reported, as the sequence number.
 */
public class SdeV2ArchivePathFactory implements ArchivePathFactory {
	private final StandardArchivePathFactory standard;
	private final Pattern buildNumberPattern;

	public SdeV2ArchivePathFactory(StandardArchivePathFactory standard, String variant) {
		this.standard = standard;
		this.buildNumberPattern = Pattern.compile("ccp/sde/\\d{4}/eve-online-static-data-(\\d+)-" + variant + "\\.zip");
	}

	@Override
	public String getName() {
		return standard.getName();
	}

	@Override
	public String getFolder() {
		return standard.getFolder();
	}

	@Override
	public String createLatestPath() {
		return standard.createLatestPath();
	}

	@Override
	public String createArchivePath(LocalDate datestamp) {
		return standard.createArchivePath(datestamp);
	}

	@Override
	public String createArchivePath(Instant timestamp) {
		return standard.createArchivePath(timestamp);
	}

	@Override
	public String createArchivePath(ZonedDateTime archiveTime) {
		return standard.createArchivePath(archiveTime);
	}

	@Override
	public Instant parseArchiveTime(String path) {
		return standard.parseArchiveTime(path);
	}

	@Override
	public Long parseSequenceNumber(String path) {
		var matcher = buildNumberPattern.matcher(stripBaseUrl(path));
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
