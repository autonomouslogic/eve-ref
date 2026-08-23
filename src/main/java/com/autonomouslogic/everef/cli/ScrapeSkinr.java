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
import com.autonomouslogic.everef.util.archive.ArchivePathFactory;
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

		var details = loadExistingDetails(existingDetailsBytes);
		var listingsMap = new LinkedHashMap<String, ObjectNode>();
		Set<String> purgedSkinrIds;
		String outputCursor;

		if (existingListingsBytes == null) {
			var page = fetchListingsPage(null);
			addListingsToMap(listingsMap, extractListings(page));
			outputCursor = extractCursorAfter(page);
			purgedSkinrIds = Set.of();
		} else {
			var existingJson = (ObjectNode) objectMapper.readTree(existingListingsBytes);
			loadListingsIntoMap(existingJson, listingsMap);
			purgedSkinrIds = purgeNonListedEntries(listingsMap);
			outputCursor = fetchIncrementalListings(extractCursorAfter(existingJson), listingsMap);
		}

		fetchMissingDetails(listingsMap, details);
		purgeOrphanedDetails(purgedSkinrIds, details);

		uploadFiles(buildListingsOutput(listingsMap, outputCursor), details);
		log.info("SKINR scrape complete");
	}

	// --- Listings loading ---

	@SneakyThrows
	private Map<String, ObjectNode> loadExistingDetails(byte[] bytes) {
		if (bytes == null) return new HashMap<>();
		return objectMapper.readValue(bytes, new TypeReference<Map<String, ObjectNode>>() {});
	}

	private void loadListingsIntoMap(ObjectNode existingJson, Map<String, ObjectNode> listingsMap) {
		var arr = existingJson.get("listings");
		if (arr != null && arr.isArray()) {
			arr.forEach(n -> {
				var obj = (ObjectNode) n;
				listingsMap.put(obj.get("id").asText(), obj);
			});
		}
	}

	private void addListingsToMap(Map<String, ObjectNode> listingsMap, List<ObjectNode> listings) {
		listings.forEach(l -> listingsMap.put(l.get("id").asText(), l));
	}

	// --- Incremental fetch ---

	@SneakyThrows
	private String fetchIncrementalListings(String startCursor, Map<String, ObjectNode> listingsMap) {
		String outputCursor = startCursor;
		String currentCursor = startCursor;
		while (true) {
			var page = fetchListingsPage(currentCursor);
			var newListings = extractListings(page);
			if (newListings.isEmpty()) break;
			addListingsToMap(listingsMap, newListings);
			var pageCursor = extractCursorAfter(page);
			if (pageCursor != null) {
				outputCursor = pageCursor;
				currentCursor = pageCursor;
			} else {
				break;
			}
		}
		return outputCursor;
	}

	// --- Purging ---

	private Set<String> purgeNonListedEntries(Map<String, ObjectNode> listingsMap) {
		var purged = new HashSet<String>();
		var iter = listingsMap.entrySet().iterator();
		while (iter.hasNext()) {
			var entry = iter.next();
			if (!"listed".equals(entry.getValue().get("state").asText())) {
				purged.add(entry.getValue().get("skinr_id").asText());
				iter.remove();
			}
		}
		return purged;
	}

	private void purgeOrphanedDetails(Set<String> purgedSkinrIds, Map<String, ObjectNode> details) {
		log.trace("Purging {} orphaned listings", purgedSkinrIds.size());
		purgedSkinrIds.forEach(details::remove);
	}

	// --- Detail fetching ---

	private void fetchMissingDetails(Map<String, ObjectNode> listingsMap, Map<String, ObjectNode> details) {
		for (var listing : listingsMap.values()) {
			var skinrId = listing.get("skinr_id").asText();
			if (!details.containsKey(skinrId)) {
				log.trace("Fetching SKINR ID " + skinrId);
				var detail = fetchDetail(skinrId);
				if (detail != null) {
					details.put(skinrId, detail);
				} else {
					log.warn("SKINR ID " + skinrId + " not found");
				}
			}
		}
	}

	// --- Output building ---

	private ObjectNode buildListingsOutput(Map<String, ObjectNode> listingsMap, String cursor) {
		var output = objectMapper.createObjectNode();
		var arr = objectMapper.createArrayNode();
		listingsMap.values().forEach(arr::add);
		output.set("listings", arr);
		if (cursor != null) {
			var cursorNode = objectMapper.createObjectNode();
			cursorNode.put("after", cursor);
			output.set("cursor", cursorNode);
		}
		return output;
	}

	// --- Upload ---

	@SneakyThrows
	private void uploadFiles(ObjectNode listings, Map<String, ObjectNode> details) {
		uploadFile("skinr-listings", listings, SKINR_LISTINGS);
		uploadFile("skinr-details", details, SKINR_DETAILS);
	}

	@SneakyThrows
	private void uploadFile(String prefix, Object data, ArchivePathFactory factory) {
		var json = tempFiles.tempFile(prefix, ".json").toFile();
		objectMapper.writeValue(json, data);
		var archive = CompressUtil.compressBzip2(json);
		s3Util.uploadLatestAndArchive(
				json, archive, dataPath, factory, scrapeTime, "application/json", "application/x-bzip2", s3Client);
	}

	// --- ESI fetch helpers ---

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
	private ObjectNode fetchDetail(String skinrId) {
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
