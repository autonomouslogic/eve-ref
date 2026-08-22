package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.archive.ArchivePathFactories.SKINR_DETAILS;
import static com.autonomouslogic.everef.util.archive.ArchivePathFactories.SKINR_LISTINGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * End-to-end tests for {@link ScrapeSkinr}. Each test is self-contained: it configures the ESI mock,
 * runs the scrape, and asserts the resulting S3 files and ESI request paths.
 *
 * <p>The scraper has two output files:
 * <ul>
 *   <li><b>Listings</b> ({@code skinr-listings/}): a snapshot of active Paragon Hub listings,
 *       merged incrementally via cursor-based pagination.
 *   <li><b>Details</b> ({@code skinr-details/}): accumulated SKINR cosmetic detail records, keyed
 *       by skinr_id, never re-fetched once cached.
 * </ul>
 *
 * <p>First-run vs subsequent-run behaviour differs:
 * <ul>
 *   <li><b>First run</b> (no previous file): a single call to {@code /paragon-hub/skinr?limit=100}
 *       returns all listings; stored as-is with no merging or purging.
 *   <li><b>Subsequent run</b>: cursor-based incremental fetch; pages merged into the previous
 *       listings map; non-"listed" entries purged after merge.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@Timeout(30)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeSkinrTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
public class ScrapeSkinrTest {
	static final String BUCKET_NAME = "data-bucket";
	static final ZonedDateTime SCRAPE_TIME = ZonedDateTime.parse("2020-02-03T04:05:06Z");

	static final String LATEST_LISTINGS_FILE = "base/" + SKINR_LISTINGS.createLatestPath();
	static final String ARCHIVE_LISTINGS_FILE = "base/" + SKINR_LISTINGS.createArchivePath(SCRAPE_TIME);
	static final String LATEST_DETAILS_FILE = "base/" + SKINR_DETAILS.createLatestPath();
	static final String ARCHIVE_DETAILS_FILE = "base/" + SKINR_DETAILS.createArchivePath(SCRAPE_TIME);

	@Inject
	ScrapeSkinr scrapeSkinr;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	DataIndexHelper dataIndexHelper;

	MockWebServer server;
	TestDispatcher dispatcher;
	List<String> requestPaths;

	@Inject
	protected ScrapeSkinrTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		dispatcher = new TestDispatcher();
		server = new MockWebServer();
		server.setDispatcher(dispatcher);
		server.start(TestDataUtil.TEST_PORT);
		scrapeSkinr.setScrapeTime(SCRAPE_TIME);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	// --- First-run tests ---

	/**
	 * First run with no previous files. A single ESI call fetches all listings; they are stored
	 * as-is. Details are fetched for every skinr_id in the returned listings.
	 */
	@Test
	@SneakyThrows
	void firstRunStoresListingsAsIs() {
		var listing = listing(1, "listed", 101);
		dispatcher
				.withFirstPageResponse(listingsPage(List.of(listing), "cursor-1", null))
				.withDetail(101, detail(101, "Wrathful Gaze Alpha"));

		run();

		var listings = readLatestListings();
		assertEquals(List.of(listing), listingsArray(listings));
		assertEquals("cursor-1", listings.get("cursor").get("after").asText());

		assertRequestPaths(
				"/cosmetics/skinr/101",
				"/paragon-hub/skinr?limit=100",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * First run: non-"listed" listings (e.g. sold_out) are stored without purging — purge only
	 * happens during the merge step on subsequent runs.
	 */
	@Test
	@SneakyThrows
	void firstRunDoesNotPurgeNonListedEntries() {
		var active = listing(1, "listed", 101);
		var soldOut = listing(2, "sold_out", 102);
		dispatcher
				.withFirstPageResponse(listingsPage(List.of(active, soldOut), "cursor-1", null))
				.withDetail(101, detail(101, "Alpha"))
				.withDetail(102, detail(102, "Beta"));

		run();

		var listings = readLatestListings();
		assertEquals(Set.of(1L, 2L), listingIds(listings));

		assertRequestPaths(
				"/cosmetics/skinr/101",
				"/cosmetics/skinr/102",
				"/paragon-hub/skinr?limit=100",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * First run: details are fetched for every distinct skinr_id present in the returned listings.
	 * The details file is keyed by skinr_id string.
	 */
	@Test
	@SneakyThrows
	void firstRunFetchesDetailForEachSkinrId() {
		var listings = List.of(listing(1, "listed", 101), listing(2, "listed", 102), listing(3, "listed", 103));
		dispatcher
				.withFirstPageResponse(listingsPage(listings, "cursor-1", null))
				.withDetail(101, detail(101, "Alpha"))
				.withDetail(102, detail(102, "Beta"))
				.withDetail(103, detail(103, "Gamma"));

		run();

		var details = readLatestDetails();
		assertEquals(Set.of("101", "102", "103"), details.keySet());
		assertEquals("Alpha", details.get("101").get("name").asText());

		assertRequestPaths(
				"/cosmetics/skinr/101",
				"/cosmetics/skinr/102",
				"/cosmetics/skinr/103",
				"/paragon-hub/skinr?limit=100",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * When multiple listings share the same skinr_id, the detail endpoint is called only once.
	 * The dispatcher returns 404 on any second call; if the command called it twice it would fail.
	 */
	@Test
	@SneakyThrows
	void firstRunFetchesEachSkinrDetailOnlyOnce() {
		var listings = List.of(listing(1, "listed", 101), listing(2, "listed", 101));
		dispatcher
				.withFirstPageResponse(listingsPage(listings, "cursor-1", null))
				.withDetailOnce(101, detail(101, "Alpha"));

		run();

		var details = readLatestDetails();
		assertEquals(1, details.size());
		assertTrue(details.containsKey("101"));

		assertRequestPaths(
				"/cosmetics/skinr/101",
				"/paragon-hub/skinr?limit=100",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	// --- Subsequent-run tests (cursor-based incremental fetch) ---

	/**
	 * Subsequent run: previous file has cursor. ESI returns empty listings immediately (no new
	 * data). Previous listings preserved unchanged; cursor in output stays the same as before.
	 */
	@Test
	@SneakyThrows
	void subsequentRunPreservesListingsWhenNoNewData() {
		var existing = listing(1, "listed", 101);
		dispatcher
				.withExistingListings(fullListingsJson(List.of(existing), "cursor-0", null))
				.withExistingDetails(detailsJson(Map.of("101", detail(101, "Alpha"))))
				.withIncrementalPage("cursor-0", listingsPage(List.of(), "cursor-1", "cursor-0"));

		run();

		var listings = readLatestListings();
		assertEquals(Set.of(1L), listingIds(listings));
		assertEquals("cursor-0", listings.get("cursor").get("after").asText());

		assertRequestPaths(
				"/paragon-hub/skinr?limit=100&after=cursor-0",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * Subsequent run: new listings are returned on the first incremental page, then an empty page
	 * signals the end. New listings are merged with previous; cursor updated to last non-empty page.
	 */
	@Test
	@SneakyThrows
	void subsequentRunMergesNewListingsWithPrevious() {
		var existing = listing(1, "listed", 101);
		var incoming = listing(2, "listed", 102);
		dispatcher
				.withExistingListings(fullListingsJson(List.of(existing), "cursor-0", null))
				.withExistingDetails(detailsJson(Map.of("101", detail(101, "Alpha"))))
				.withIncrementalPage("cursor-0", listingsPage(List.of(incoming), "cursor-1", "cursor-0"))
				.withIncrementalPage("cursor-1", listingsPage(List.of(), "cursor-2", "cursor-1"))
				.withDetail(102, detail(102, "Beta"));

		run();

		var listings = readLatestListings();
		assertEquals(Set.of(1L, 2L), listingIds(listings));
		assertEquals("cursor-1", listings.get("cursor").get("after").asText());

		assertRequestPaths(
				"/cosmetics/skinr/102",
				"/paragon-hub/skinr?limit=100&after=cursor-0",
				"/paragon-hub/skinr?limit=100&after=cursor-1",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * Subsequent run: the cursor advances through multiple non-empty incremental pages before the
	 * empty page halts pagination. Output cursor equals the last non-empty page's cursor.
	 */
	@Test
	@SneakyThrows
	void subsequentRunCursorAdvancedToLastNonEmptyPage() {
		var existing = listing(1, "listed", 101);
		var page1 = listing(2, "listed", 102);
		var page2 = listing(3, "listed", 103);
		dispatcher
				.withExistingListings(fullListingsJson(List.of(existing), "cursor-0", null))
				.withExistingDetails(detailsJson(Map.of("101", detail(101, "Alpha"))))
				.withIncrementalPage("cursor-0", listingsPage(List.of(page1), "cursor-1", "cursor-0"))
				.withIncrementalPage("cursor-1", listingsPage(List.of(page2), "cursor-2", "cursor-1"))
				.withIncrementalPage("cursor-2", listingsPage(List.of(), "cursor-3", "cursor-2"))
				.withDetail(102, detail(102, "Beta"))
				.withDetail(103, detail(103, "Gamma"));

		run();

		var listings = readLatestListings();
		assertEquals(Set.of(1L, 2L, 3L), listingIds(listings));
		assertEquals("cursor-2", listings.get("cursor").get("after").asText());

		assertRequestPaths(
				"/cosmetics/skinr/102",
				"/cosmetics/skinr/103",
				"/paragon-hub/skinr?limit=100&after=cursor-0",
				"/paragon-hub/skinr?limit=100&after=cursor-1",
				"/paragon-hub/skinr?limit=100&after=cursor-2",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * Subsequent run: a listing returned in an incremental page with the same id as an existing
	 * entry replaces the existing entry. Newer data from the API supersedes older stored data.
	 */
	@Test
	@SneakyThrows
	void subsequentRunNewDataOverridesExistingById() {
		var existingVersion = listing(1, "listed", 101).put("quantity", 1);
		var updatedVersion = listing(1, "listed", 101).put("quantity", 99);
		dispatcher
				.withExistingListings(fullListingsJson(List.of(existingVersion), "cursor-0", null))
				.withExistingDetails(detailsJson(Map.of("101", detail(101, "Alpha"))))
				.withIncrementalPage("cursor-0", listingsPage(List.of(updatedVersion), "cursor-1", "cursor-0"))
				.withIncrementalPage("cursor-1", listingsPage(List.of(), "cursor-2", "cursor-1"));

		run();

		var listings = readLatestListings();
		var mergedList = listingsArray(listings);
		assertEquals(1, mergedList.size());
		assertEquals(99, mergedList.get(0).get("quantity").asInt());

		assertRequestPaths(
				"/paragon-hub/skinr?limit=100&after=cursor-0",
				"/paragon-hub/skinr?limit=100&after=cursor-1",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	// --- Purge tests ---

	/**
	 * After merging incremental data, listings with state != "listed" (sold_out, expired, removed)
	 * are purged from the output. This applies to entries from the previous file.
	 */
	@Test
	@SneakyThrows
	void subsequentRunPurgesNonListedFromPreviousFile() {
		var active = listing(1, "listed", 101);
		var soldOut = listing(2, "sold_out", 102);
		var expired = listing(3, "expired", 103);
		var removed = listing(4, "removed", 104);
		dispatcher
				.withExistingListings(fullListingsJson(List.of(active, soldOut, expired, removed), "cursor-0", null))
				.withExistingDetails(detailsJson(Map.of(
						"101", detail(101, "A"),
						"102", detail(102, "B"),
						"103", detail(103, "C"),
						"104", detail(104, "D"))))
				.withIncrementalPage("cursor-0", listingsPage(List.of(), "cursor-1", "cursor-0"));

		run();

		var listings = readLatestListings();
		assertEquals(Set.of(1L), listingIds(listings));

		assertRequestPaths(
				"/paragon-hub/skinr?limit=100&after=cursor-0",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * When a new incremental page updates an existing listing's state to non-"listed", the merge
	 * inserts the updated entry and the purge step then removes it.
	 */
	@Test
	@SneakyThrows
	void subsequentRunPurgesListingUpdatedToNonListedState() {
		var original = listing(1, "listed", 101);
		var soldOut = listing(1, "sold_out", 101);
		dispatcher
				.withExistingListings(fullListingsJson(List.of(original), "cursor-0", null))
				.withExistingDetails(detailsJson(Map.of("101", detail(101, "Alpha"))))
				.withIncrementalPage("cursor-0", listingsPage(List.of(soldOut), "cursor-1", "cursor-0"))
				.withIncrementalPage("cursor-1", listingsPage(List.of(), "cursor-2", "cursor-1"));

		run();

		var listings = readLatestListings();
		assertEquals(Set.of(), listingIds(listings));

		assertRequestPaths(
				"/paragon-hub/skinr?limit=100&after=cursor-0",
				"/paragon-hub/skinr?limit=100&after=cursor-1",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	// --- Details accumulation tests ---

	/**
	 * skinr_ids already present in the existing details file are not re-fetched from ESI.
	 * The dispatcher returns 404 for a second call to any already-known skinr_id;
	 * if the command re-fetched it, the test would fail with an unexpected 404.
	 */
	@Test
	@SneakyThrows
	void subsequentRunSkipsAlreadyKnownSkinrDetails() {
		var existing = listing(1, "listed", 101);
		var incoming = listing(2, "listed", 102);
		dispatcher
				.withExistingListings(fullListingsJson(List.of(existing), "cursor-0", null))
				.withExistingDetails(detailsJson(Map.of("101", detail(101, "Alpha"))))
				.withIncrementalPage("cursor-0", listingsPage(List.of(incoming), "cursor-1", "cursor-0"))
				.withIncrementalPage("cursor-1", listingsPage(List.of(), "cursor-2", "cursor-1"))
				.withDetail(102, detail(102, "Beta"));
		// skinr_id 101 is NOT registered — a fetch would return 404 causing an error

		run();

		var details = readLatestDetails();
		assertEquals(Set.of("101", "102"), details.keySet());

		assertRequestPaths(
				"/cosmetics/skinr/102",
				"/paragon-hub/skinr?limit=100&after=cursor-0",
				"/paragon-hub/skinr?limit=100&after=cursor-1",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * Details from the previous file are merged with newly fetched details in the output.
	 * Both previously known and newly fetched entries appear in the final details file.
	 */
	@Test
	@SneakyThrows
	void subsequentRunMergesOldAndNewDetails() {
		var existing = listing(1, "listed", 101);
		var incoming = listing(2, "listed", 102);
		dispatcher
				.withExistingListings(fullListingsJson(List.of(existing), "cursor-0", null))
				.withExistingDetails(detailsJson(Map.of("101", detail(101, "Alpha"))))
				.withIncrementalPage("cursor-0", listingsPage(List.of(incoming), "cursor-1", "cursor-0"))
				.withIncrementalPage("cursor-1", listingsPage(List.of(), "cursor-2", "cursor-1"))
				.withDetail(102, detail(102, "Beta"));

		run();

		var details = readLatestDetails();
		assertEquals("Alpha", details.get("101").get("name").asText());
		assertEquals("Beta", details.get("102").get("name").asText());

		assertRequestPaths(
				"/cosmetics/skinr/102",
				"/paragon-hub/skinr?limit=100&after=cursor-0",
				"/paragon-hub/skinr?limit=100&after=cursor-1",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	// --- S3 upload / data index tests ---

	/**
	 * Both latest and archive files are written to S3 for listings and details after a successful run.
	 */
	@Test
	@SneakyThrows
	void bothFilesUploadedToS3() {
		dispatcher
				.withFirstPageResponse(listingsPage(List.of(listing(1, "listed", 101)), "cursor-1", null))
				.withDetail(101, detail(101, "Alpha"));

		run();

		assertTrue(mockS3Adapter
				.getTestObject(BUCKET_NAME, LATEST_LISTINGS_FILE, dataClient)
				.isPresent());
		assertTrue(mockS3Adapter
				.getTestObject(BUCKET_NAME, ARCHIVE_LISTINGS_FILE, dataClient)
				.isPresent());
		assertTrue(mockS3Adapter
				.getTestObject(BUCKET_NAME, LATEST_DETAILS_FILE, dataClient)
				.isPresent());
		assertTrue(mockS3Adapter
				.getTestObject(BUCKET_NAME, ARCHIVE_DETAILS_FILE, dataClient)
				.isPresent());

		assertRequestPaths(
				"/cosmetics/skinr/101",
				"/paragon-hub/skinr?limit=100",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	/**
	 * Data index is updated for both the listings file and the details file after each run.
	 */
	@Test
	@SneakyThrows
	void dataIndexUpdatedForBothFiles() {
		dispatcher
				.withFirstPageResponse(listingsPage(List.of(listing(1, "listed", 101)), "cursor-1", null))
				.withDetail(101, detail(101, "Alpha"));

		run();

		verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder()
								.bucket(BUCKET_NAME)
								.path(LATEST_LISTINGS_FILE)
								.build(),
						S3Url.builder()
								.bucket(BUCKET_NAME)
								.path(ARCHIVE_LISTINGS_FILE)
								.build());
		verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder()
								.bucket(BUCKET_NAME)
								.path(LATEST_DETAILS_FILE)
								.build(),
						S3Url.builder()
								.bucket(BUCKET_NAME)
								.path(ARCHIVE_DETAILS_FILE)
								.build());

		assertRequestPaths(
				"/cosmetics/skinr/101",
				"/paragon-hub/skinr?limit=100",
				"/skinr-details/skinr-details-latest.json",
				"/skinr-listings/skinr-listings-latest.json");
	}

	// --- Run helper ---

	@SneakyThrows
	private void run() {
		scrapeSkinr.run();
		var raw = new ArrayList<RecordedRequest>();
		RecordedRequest req;
		while ((req = server.takeRequest(1, TimeUnit.MILLISECONDS)) != null) {
			raw.add(req);
		}
		requestPaths =
				raw.stream().map(RecordedRequest::getPath).sorted().distinct().toList();
	}

	// --- Output readers ---

	@SneakyThrows
	private ObjectNode readLatestListings() {
		var bytes = mockS3Adapter
				.getTestObject(BUCKET_NAME, LATEST_LISTINGS_FILE, dataClient)
				.orElseThrow(() -> new AssertionError("Latest listings file not found in S3"));
		return (ObjectNode) objectMapper.readTree(bytes);
	}

	@SneakyThrows
	private Map<String, ObjectNode> readLatestDetails() {
		var bytes = mockS3Adapter
				.getTestObject(BUCKET_NAME, LATEST_DETAILS_FILE, dataClient)
				.orElseThrow(() -> new AssertionError("Latest details file not found in S3"));
		return objectMapper.readValue(bytes, new TypeReference<Map<String, ObjectNode>>() {});
	}

	// --- Assertion helpers ---

	private void assertRequestPaths(String... paths) {
		assertEquals(List.of(paths), requestPaths);
	}

	private List<ObjectNode> listingsArray(ObjectNode listingsResponse) {
		var arr = (ArrayNode) listingsResponse.get("listings");
		var result = new ArrayList<ObjectNode>();
		arr.forEach(n -> result.add((ObjectNode) n));
		result.sort((a, b) -> Long.compare(a.get("id").asLong(), b.get("id").asLong()));
		return result;
	}

	private Set<Long> listingIds(ObjectNode listingsResponse) {
		var ids = new HashSet<Long>();
		listingsResponse.get("listings").forEach(n -> ids.add(n.get("id").asLong()));
		return ids;
	}

	// --- Data builders ---

	private ObjectNode listing(long id, String state, int skinrId) {
		return objectMapper
				.createObjectNode()
				.put("id", id)
				.put("state", state)
			.put("last_modified", "2024-01-01T00:00:00Z")
			.put("seller_id", 1_000_000L + id)
				.put("skinr_id", skinrId)
				.put("created", "2024-01-01T00:00:00Z")
				.put("expires", "2024-02-01T00:00:00Z")
				.put("quantity", 1)
				.set("price", objectMapper.createObjectNode().put("isk", 425000000));
	}

	private ObjectNode detail(int id, String name) {
		var obj = objectMapper
				.createObjectNode()
				.put("id", id)
				.put("name", name)
				.put("creator_id", 2_000_000 + id)
				.put("ship_type_id", 587)
				.put("line", "Wrathful Gaze");
		obj.set("layout", objectMapper.createObjectNode()
					.put("pattern_blend_mode",  "normal"));
		obj.set("tier", objectMapper.createObjectNode().put("level", 6));
		return obj;
	}

	@SneakyThrows
	private String listingsPage(List<ObjectNode> listings, String afterCursor, String beforeCursor) {
		var array = objectMapper.createArrayNode();
		listings.forEach(array::add);
		var cursor = objectMapper.createObjectNode();
		if (afterCursor != null) cursor.put("after", afterCursor);
		if (beforeCursor != null) cursor.put("before", beforeCursor);
		var root = objectMapper.createObjectNode();
		root.set("listings", array);
		root.set("cursor", cursor);
		return objectMapper.writeValueAsString(root);
	}

	@SneakyThrows
	private String fullListingsJson(List<ObjectNode> listings, String afterCursor, String beforeCursor) {
		return listingsPage(listings, afterCursor, beforeCursor);
	}

	@SneakyThrows
	private String detailsJson(Map<String, ObjectNode> details) {
		return objectMapper.writeValueAsString(details);
	}

	// --- Test dispatcher ---

	class TestDispatcher extends Dispatcher {
		private String firstPageResponse = null;
		// key: after-cursor value; value: response JSON
		private final Map<String, String> incrementalPages = new HashMap<>();
		// key: skinr_id; value: response JSON
		private final Map<Integer, String> detailResponses = new HashMap<>();
		// skinr_ids served only once; 404 on any second call
		private final Set<Integer> onceOnlyIds = new HashSet<>();
		private final Set<Integer> servedOnceIds = Collections.synchronizedSet(new HashSet<>());
		private String existingListings = null;
		private String existingDetails = null;

		TestDispatcher withFirstPageResponse(String responseJson) {
			this.firstPageResponse = responseJson;
			return this;
		}

		TestDispatcher withIncrementalPage(String afterCursor, String responseJson) {
			incrementalPages.put(afterCursor, responseJson);
			return this;
		}

		/** Detail returned every time the endpoint is called. */
		TestDispatcher withDetail(int skinrId, ObjectNode detail) {
			try {
				detailResponses.put(skinrId, objectMapper.writeValueAsString(detail));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return this;
		}

		/** Detail returned only on the first call; subsequent calls get 404. */
		TestDispatcher withDetailOnce(int skinrId, ObjectNode detail) {
			try {
				detailResponses.put(skinrId, objectMapper.writeValueAsString(detail));
				onceOnlyIds.add(skinrId);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return this;
		}

		TestDispatcher withExistingListings(String json) {
			this.existingListings = json;
			return this;
		}

		TestDispatcher withExistingDetails(String json) {
			this.existingDetails = json;
			return this;
		}

		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) {
			try {
				var url = request.getRequestUrl();
				var path = url.encodedPath();

				var compatibilityDate = request.getHeader("X-Compatibility-Date");
				if (compatibilityDate == null || compatibilityDate.isBlank()) {
					log.error("Missing X-Compatibility-Date header on request: {}", path);
					return new MockResponse().setResponseCode(404);
				}

				// Existing-file downloads (DATA_BASE_URL)
				if (path.equals("/" + SKINR_LISTINGS.createLatestPath())) {
					return existingListings != null
							? mockJson(existingListings)
							: new MockResponse().setResponseCode(404);
				}
				if (path.equals("/" + SKINR_DETAILS.createLatestPath())) {
					return existingDetails != null
							? mockJson(existingDetails)
							: new MockResponse().setResponseCode(404);
				}

				// Paragon Hub listings endpoint
				if (path.equals("/paragon-hub/skinr")) {
					var after = url.queryParameter("after");
					if (after == null) {
						// First run — no previous file
						return firstPageResponse != null
								? mockJson(firstPageResponse)
								: new MockResponse().setResponseCode(404);
					} else {
						// Subsequent run — cursor-based incremental
						var body = incrementalPages.get(after);
						return body != null ? mockJson(body) : new MockResponse().setResponseCode(404);
					}
				}

				// SKINR detail endpoint
				if (path.startsWith("/cosmetics/skinr/")) {
					var skinrId = Integer.parseInt(path.substring("/cosmetics/skinr/".length()));
					if (onceOnlyIds.contains(skinrId)) {
						if (servedOnceIds.add(skinrId)) {
							return mockJson(detailResponses.get(skinrId));
						}
						return new MockResponse().setResponseCode(404);
					}
					if (detailResponses.containsKey(skinrId)) {
						return mockJson(detailResponses.get(skinrId));
					}
					return new MockResponse().setResponseCode(404);
				}

				log.error("Unaccounted URL: {} query={}", path, url.query());
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Dispatcher error", e);
				return new MockResponse().setResponseCode(500).setBody(e.getMessage());
			}
		}

		private MockResponse mockJson(String json) {
			return new MockResponse()
					.setResponseCode(200)
					.addHeader("Content-Type", "application/json")
					.setBody(json);
		}
	}
}
