package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.MER;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.DiscordNotifier;
import com.autonomouslogic.everef.util.TempFiles;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Syncs Monthly Economic Reports (MER) from CCP's CDN.
 */
@Log4j2
public class SyncMer implements Command {
	private static final YearMonth MER_FIRST_MONTH = YearMonth.of(2025, 8);

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected S3Util s3Util;

	@Inject
	@Named("data")
	protected S3AsyncClient s3Client;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected DataIndexHelper dataIndexHelper;

	@Inject
	protected DiscordNotifier discordNotifier;

	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private final URI merBaseUrl = Configs.MER_BASE_URL.getRequired();
	private S3Url dataPath;

	@Inject
	protected SyncMer() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
	}

	@Override
	public void run() {
		var existingFiles = scanExistingMerFiles();
		log.info("Found {} existing MER files", existingFiles.size());

		var monthsToCheck = determineMonthsToCheck(existingFiles);
		log.info("Checking {} months for new MER files: {}", monthsToCheck.size(), monthsToCheck);

		var uploadedPaths = new ArrayList<S3Url>();
		for (var month : monthsToCheck) {
			syncMonth(month, uploadedPaths);
		}

		if (!uploadedPaths.isEmpty()) {
			log.info("Updating data index for {} uploaded files", uploadedPaths.size());
			dataIndexHelper.updateIndex(uploadedPaths).blockingAwait();
		}
	}

	@SneakyThrows
	private Set<YearMonth> scanExistingMerFiles() {
		var existing = new HashSet<YearMonth>();
		var merPath = dataPath.resolve(MER.getFolder() + "/");
		var objects = s3Adapter.listObjects(merPath, true, s3Client);

		for (var object : objects) {
			var path = object.getUrl().getPath();
			var instant = MER.parseArchiveTime(path);
			if (instant != null) {
				var yearMonth = YearMonth.from(instant.atZone(ZoneOffset.UTC));
				existing.add(yearMonth);
			}
		}

		return existing;
	}

	private List<YearMonth> determineMonthsToCheck(Set<YearMonth> existing) {
		var startMonth =
				existing.isEmpty() ? MER_FIRST_MONTH : getLatestMonth(existing).minusMonths(2);
		var endMonth = YearMonth.now(ZoneOffset.UTC);

		var monthsToCheck = new ArrayList<YearMonth>();
		var currentMonth = startMonth;
		while (!currentMonth.isAfter(endMonth)) {
			if (!existing.contains(currentMonth)) {
				monthsToCheck.add(currentMonth);
			}
			currentMonth = currentMonth.plusMonths(1);
		}

		return monthsToCheck;
	}

	private void syncMonth(YearMonth month, ArrayList<S3Url> uploadedPaths) {
		var uri = constructMerUrl(month);
		var file = tempFiles.tempFile("mer-" + month, ".zip").toFile();
		try (var response = okHttpWrapper.download(uri.toString(), file)) {
			if (response.code() == 404) {
				log.info("MER file not available yet: {}", month);
				return;
			}
			if (response.code() != 200) {
				log.warn("Failed to download MER {}: status {}", month, response.code());
				return;
			}

			var uploadedPath = uploadMerFile(file, month);
			uploadedPaths.add(uploadedPath);

			var filename = new File(uploadedPath.getPath()).getName();
			var httpUrl = URI.create(Configs.DATA_BASE_URL.getRequired() + uploadedPath.getPath());
			var discordMsg = String.format("New MER file synced: [%s](%s)", filename, httpUrl);
			discordNotifier.notifyDiscord(discordMsg);
		} catch (Exception e) {
			log.warn("Error syncing MER for month {}", month, e);
		} finally {
			file.delete();
		}
	}

	private YearMonth getLatestMonth(Set<YearMonth> months) {
		return months.stream().max(YearMonth::compareTo).orElseThrow();
	}

	private URI constructMerUrl(YearMonth month) {
		var filename = new File(MER.createArchivePath(month.atDay(1))).getName();
		return merBaseUrl.resolve(filename);
	}

	private String formatYearMonth(YearMonth month) {
		return String.format("%04d%02d", month.getYear(), month.getMonthValue());
	}

	@SneakyThrows
	private S3Url uploadMerFile(File file, YearMonth month) {
		var merPath = MER.createArchivePath(month.atDay(1).atStartOfDay(ZoneOffset.UTC));
		var archivePath = dataPath.resolve(merPath);

		var putRequest = s3Util.putPublicObjectRequest(file.length(), archivePath, archiveCacheTime);
		log.info("Uploading MER file {} to {}", month, archivePath);
		s3Adapter.putObject(putRequest, file, s3Client);

		return archivePath;
	}
}
