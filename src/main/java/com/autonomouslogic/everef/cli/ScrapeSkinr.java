package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.archive.ArchivePathFactories.SKINR_DETAILS;
import static com.autonomouslogic.everef.util.archive.ArchivePathFactories.SKINR_LISTINGS;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Log4j2
public class ScrapeSkinr implements Command {
	private static final int LIMIT = 100;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected S3Util s3Util;

	@Inject
	@Named("data")
	protected S3AsyncClient s3Client;

	@Inject
	protected TempFiles tempFiles;

	@Setter
	private ZonedDateTime scrapeTime;

	private S3Url dataPath;
	private URI dataBaseUrl;

	@Inject
	protected ScrapeSkinr() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
	}

	@Override
	@SneakyThrows
	public void run() {
		if (scrapeTime == null) {
			scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		}
		log.info("Starting SKINR scrape");

		var existingListingsBytes = downloadExistingFile(SKINR_LISTINGS.createLatestPath());
		var existingDetailsBytes = downloadExistingFile(SKINR_DETAILS.createLatestPath());

		Map<String, ObjectNode> details = new HashMap<>();
		if (existingDetailsBytes != null) {
			details = objectMapper.readValue(existingDetailsBytes, new TypeReference<Map<String, ObjectNode>>() {});
		}

		Map<Long, ObjectNode> listingsMap = new LinkedHashMap<>();
		String outputCursor;
		Set<Integer> purgedSkinrIds = new HashSet<>();

		if (existingListingsBytes == null) {
			var page = fetchListingsPage(null);
			for (var listing : extractListings(page)) {
				listingsMap.put(listing.get("id").asLong(), listing);
			}
			outputCursor = extractCursorAfter(page);
		} else {
			var existingJson = (ObjectNode) objectMapper.readTree(existingListingsBytes);
			String existingCursor = extractCursorAfter(existingJson);

			var existingArr = existingJson.get("listings");
			if (existingArr != null && existingArr.isArray()) {
				existingArr.forEach(n -> {
					var obj = (ObjectNode) n;
					listingsMap.put(obj.get("id").asLong(), obj);
				});
			}

			var iter = listingsMap.entrySet().iterator();
			while (iter.hasNext()) {
				var entry = iter.next();
				if (!"listed".equals(entry.getValue().get("state").asText())) {
					purgedSkinrIds.add(entry.getValue().get("skinr_id").asInt());
					iter.remove();
				}
			}

			outputCursor = existingCursor;
			String currentCursor = existingCursor;
			while (true) {
				var page = fetchListingsPage(currentCursor);
				var newListings = extractListings(page);
				if (newListings.isEmpty()) {
					break;
				}
				for (var listing : newListings) {
					listingsMap.put(listing.get("id").asLong(), listing);
				}
				var pageCursor = extractCursorAfter(page);
				if (pageCursor != null) {
					outputCursor = pageCursor;
					currentCursor = pageCursor;
				} else {
					break;
				}
			}
		}

		for (var listing : listingsMap.values()) {
			var skinrId = listing.get("skinr_id").asInt();
			var key = String.valueOf(skinrId);
			if (!details.containsKey(key)) {
				var detail = fetchDetail(skinrId);
				if (detail != null) {
					details.put(key, detail);
				}
			}
		}

		for (var skinrId : purgedSkinrIds) {
			details.remove(String.valueOf(skinrId));
		}

		var listingsOutput = objectMapper.createObjectNode();
		var listingsArr = objectMapper.createArrayNode();
		listingsMap.values().forEach(listingsArr::add);
		listingsOutput.set("listings", listingsArr);
		if (outputCursor != null) {
			var cursorNode = objectMapper.createObjectNode();
			cursorNode.put("after", outputCursor);
			listingsOutput.set("cursor", cursorNode);
		}

		uploadFiles(listingsOutput, details);
		log.info("SKINR scrape complete");
	}

	@SneakyThrows
	private void uploadFiles(ObjectNode listings, Map<String, ObjectNode> details) {
		var listingsJson = tempFiles.tempFile("skinr-listings", ".json").toFile();
		objectMapper.writeValue(listingsJson, listings);
		var listingsArchive = CompressUtil.compressBzip2(listingsJson);
		s3Util.uploadLatestAndArchive(
				listingsJson,
				listingsArchive,
				dataPath,
				SKINR_LISTINGS,
				scrapeTime,
				"application/json",
				"application/x-bzip2",
				s3Client);

		var detailsJson = tempFiles.tempFile("skinr-details", ".json").toFile();
		objectMapper.writeValue(detailsJson, details);
		var detailsArchive = CompressUtil.compressBzip2(detailsJson);
		s3Util.uploadLatestAndArchive(
				detailsJson,
				detailsArchive,
				dataPath,
				SKINR_DETAILS,
				scrapeTime,
				"application/json",
				"application/x-bzip2",
				s3Client);
	}

	@SneakyThrows
	private byte[] downloadExistingFile(String path) {
		var url = dataBaseUrl.resolve(path).toString();
		var response = okHttpWrapper.get(url);
		try (response) {
			if (response.code() == 404) {
				return null;
			}
			if (response.code() != 200) {
				throw new RuntimeException("Failed to download " + path + ": HTTP " + response.code());
			}
			return response.body().bytes();
		}
	}

	@SneakyThrows
	private JsonNode fetchListingsPage(String afterCursor) {
		var builder = EsiUrl.modern().urlPath("/paragon-hub/skinr?limit=" + LIMIT);
		if (afterCursor != null) {
			builder.after(afterCursor);
		}
		return esiHelper.decodeResponse(esiHelper.fetch(builder.build()));
	}

	@SneakyThrows
	private ObjectNode fetchDetail(int skinrId) {
		var url = EsiUrl.modern().urlPath("/cosmetics/skinr/" + skinrId).build();
		var node = esiHelper.decodeResponse(esiHelper.fetch(url));
		if (node == null || node.isNull()) {
			return null;
		}
		return (ObjectNode) node;
	}

	private List<ObjectNode> extractListings(JsonNode page) {
		var result = new ArrayList<ObjectNode>();
		if (page == null || page.isNull()) {
			return result;
		}
		var arr = page.get("listings");
		if (arr != null && arr.isArray()) {
			arr.forEach(n -> result.add((ObjectNode) n));
		}
		return result;
	}

	private String extractCursorAfter(JsonNode page) {
		if (page == null || page.isNull()) {
			return null;
		}
		var cursor = page.get("cursor");
		if (cursor == null || cursor.isNull()) {
			return null;
		}
		var after = cursor.get("after");
		if (after == null || after.isNull()) {
			return null;
		}
		return after.asText();
	}
}
