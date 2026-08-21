package com.autonomouslogic.everef.cli.publiccontracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * End-to-end tests for {@link ScrapePublicContracts}. Each test method is self-contained: it sets
 * up the ESI mock, runs the scrape, and asserts the resulting archive and ESI calls.
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapePublicContractsTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "REF_DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
public class ScrapePublicContractsTest {
	static final String BUCKET_NAME = "data-bucket";

	/** Minimal meta-group 15 (Abyssal) response with no type IDs, sufficient for non-abyssal item tests. */
	private static final String NON_ABYSSAL_META_GROUPS_JSON = "{\"meta_group_id\":15,\"type_ids\":[]}";

	private static final String ARCHIVE_FILE =
			"base/public-contracts/history/2020/2020-02-03/public-contracts-2020-02-03_04-05-06.v2.tar.bz2";
	private static final String LATEST_FILE = "base/public-contracts/public-contracts-latest.v2.tar.bz2";

	@Inject
	ScrapePublicContracts scrapePublicContracts;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	DataIndexHelper dataIndexHelper;

	@Inject
	ObjectMapper objectMapper;

	@Mock
	LocationPopulator locationPopulator;

	final String lastModified = "Mon, 03 Apr 2023 03:47:30 GMT";
	final Instant lastModifiedInstant = Instant.parse("2023-04-03T03:47:30Z");

	MockWebServer server;

	private Map<String, List<Map<String, String>>> records;
	private List<String> requestPaths;
	private byte[] content;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);
		when(locationPopulator.populate(any(), any())).thenAnswer(MockLocationPopulatorModule.mockPopulate());
		server = new MockWebServer();
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	/**
	 * No previous archive on S3. A single courier contract in one region. Courier contracts have no
	 * items or bids, so only contracts.csv should have data.
	 */
	@Test
	@SneakyThrows
	void singleCourierContractNoExistingArchive() {
		var contracts = List.of(contract(100));
		server.setDispatcher(dispatcher().withRegion(10000001).withContracts(10000001, contractsJson(contracts)));
		run();
		assertEquals(expectedContracts(contracts, 10000001), records.get("contracts.csv"));
		assertNoSubData();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * No previous archive on S3. Two courier contracts in one region, one page. Both should appear
	 * in the archive with no items or bids.
	 */
	@Test
	@SneakyThrows
	void twoCourierContractsNoExistingArchive() {
		var contracts = List.of(contract(200), contract(201));
		server.setDispatcher(dispatcher().withRegion(10000001).withContracts(10000001, contractsJson(contracts)));
		run();
		assertEquals(expectedContracts(contracts, 10000001), records.get("contracts.csv"));
		assertNoSubData();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * No previous archive on S3. One contract on each of two pages for the same region. Both should
	 * appear in the archive, verifying that pagination fetches all pages.
	 */
	@Test
	@SneakyThrows
	void paginatedCourierContractsNoExistingArchive() {
		var page1 = List.of(contract(300));
		var page2 = List.of(contract(301));
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, 1, contractsJson(page1))
				.withContracts(10000001, 2, contractsJson(page2)));
		run();
		var allContracts = new ArrayList<>(page1);
		allContracts.addAll(page2);
		assertEquals(expectedContracts(allContracts, 10000001), records.get("contracts.csv"));
		assertNoSubData();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=2",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * No previous archive on S3. One contract in each of two regions. Both should appear in the
	 * archive, verifying that all regions are scraped.
	 */
	@Test
	@SneakyThrows
	void contractsInTwoRegionsNoExistingArchive() {
		var region1Contracts = List.of(contract(400));
		var region2Contracts = List.of(contract(401));
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegion(10000002)
				.withContracts(10000001, contractsJson(region1Contracts))
				.withContracts(10000002, contractsJson(region2Contracts)));
		run();
		assertEquals(
				sortedByContractId(
						expectedContracts(region1Contracts, 10000001), expectedContracts(region2Contracts, 10000002)),
				records.get("contracts.csv"));
		assertNoSubData();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/10000002?datasource=tranquility&language=en&page=1",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/10000002/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * An existing archive contains a courier contract. The same contract is returned by the ESI.
	 * The contract should still be in the new archive, but http_last_modified should be updated.
	 */
	@Test
	@SneakyThrows
	void existingCourierContractUpdates() {
		var contract = contract(500);
		var oldLastModifiedInstant = Instant.parse("2023-03-01T00:00:00Z");

		var existingContracts = expectedContracts(List.of(contract), 10000001);
		existingContracts.forEach(m -> m.put("http_last_modified", oldLastModifiedInstant.toString()));
		var existingArchive = createExistingArchive(existingContracts);

		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contract)))
				.withLatestArchive(existingArchive));
		run();
		assertEquals(expectedContracts(List.of(contract), 10000001), records.get("contracts.csv"));
		assertNoSubData();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * An existing archive has two courier contracts. The ESI returns only one of them. The new archive
	 * should contain only the contract returned by the ESI; the other should be dropped.
	 */
	@Test
	@SneakyThrows
	void missingContractRemovedFromArchive() {
		var kept = contract(600);
		var dropped = contract(601);

		var existingContracts = expectedContracts(List.of(kept, dropped), 10000001);
		var existingArchive = createExistingArchive(existingContracts);

		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(kept)))
				.withLatestArchive(existingArchive));
		run();

		assertEquals(expectedContracts(List.of(kept), 10000001), records.get("contracts.csv"));
		assertNoSubData();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * No previous archive. One non-abyssal item on a single contract. Both item_exchange and
	 * auction contracts fetch items; auction additionally fetches bids (none configured here).
	 */
	@ParameterizedTest
	@ValueSource(strings = {"item_exchange", "auction"})
	@SneakyThrows
	void singleItemNoExistingArchive(String contractType) {
		var item = item(700001, 34);
		var contract = contract(700).put("type", contractType);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contract)))
				.withItems(700, itemsJson(List.of(item)))
				.withMetaGroups(NON_ABYSSAL_META_GROUPS_JSON));
		run();

		assertEquals(expectedContracts(List.of(contract), 10000001), records.get("contracts.csv"));
		assertEquals(expectedItems(700, List.of(item)), records.get("contract_items.csv"));
		assertNoSubDataExceptItems();
		assertLatestFileMatches();
		if ("auction".equals(contractType)) {
			assertRequestPaths(
					"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
					"/latest/contracts/public/bids/700?datasource=tranquility&language=en&page=1",
					"/latest/contracts/public/items/700?datasource=tranquility&language=en&page=1",
					"/meta_groups/15",
					"/public-contracts/public-contracts-latest.v2.tar.bz2",
					"/universe/regions/10000001/?datasource=tranquility",
					"/universe/regions/?datasource=tranquility");
		} else {
			assertRequestPaths(
					"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
					"/latest/contracts/public/items/700?datasource=tranquility&language=en&page=1",
					"/meta_groups/15",
					"/public-contracts/public-contracts-latest.v2.tar.bz2",
					"/universe/regions/10000001/?datasource=tranquility",
					"/universe/regions/?datasource=tranquility");
		}
		assertDataIndex();
	}

	/**
	 * No previous archive. Two items on two separate pages for a single contract. Both pages should
	 * be fetched and both items should appear in the archive.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"item_exchange", "auction"})
	@SneakyThrows
	void paginatedItemsNoExistingArchive(String contractType) {
		var item1 = item(800001, 34);
		var item2 = item(800002, 35);
		var contract = contract(800).put("type", contractType);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contract)))
				.withItems(800, 1, itemsJson(List.of(item1)))
				.withItems(800, 2, itemsJson(List.of(item2)))
				.withMetaGroups(NON_ABYSSAL_META_GROUPS_JSON));
		run();

		assertEquals(expectedContracts(List.of(contract), 10000001), records.get("contracts.csv"));
		assertEquals(expectedItems(800, List.of(item1, item2)), records.get("contract_items.csv"));
		assertNoSubDataExceptItems();
		assertLatestFileMatches();
		if ("auction".equals(contractType)) {
			assertRequestPaths(
					"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
					"/latest/contracts/public/bids/800?datasource=tranquility&language=en&page=1",
					"/latest/contracts/public/items/800?datasource=tranquility&language=en&page=1",
					"/latest/contracts/public/items/800?datasource=tranquility&language=en&page=2",
					"/meta_groups/15",
					"/public-contracts/public-contracts-latest.v2.tar.bz2",
					"/universe/regions/10000001/?datasource=tranquility",
					"/universe/regions/?datasource=tranquility");
		} else {
			assertRequestPaths(
					"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
					"/latest/contracts/public/items/800?datasource=tranquility&language=en&page=1",
					"/latest/contracts/public/items/800?datasource=tranquility&language=en&page=2",
					"/meta_groups/15",
					"/public-contracts/public-contracts-latest.v2.tar.bz2",
					"/universe/regions/10000001/?datasource=tranquility",
					"/universe/regions/?datasource=tranquility");
		}
		assertDataIndex();
	}

	/**
	 * No previous archive. Single auction contract with one bid. Both the contract and the bid
	 * should appear in the archive.
	 */
	@Test
	@SneakyThrows
	void singleBidNoExistingArchive() {
		var bid = bid(900001);
		var contract = contract(900).put("type", "auction");
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contract)))
				.withItems(900, itemsJson(List.of()))
				.withBids(900, bidsJson(List.of(bid)))
				.withMetaGroups(NON_ABYSSAL_META_GROUPS_JSON));
		run();

		assertEquals(expectedContracts(List.of(contract), 10000001), records.get("contracts.csv"));
		assertEquals(List.of(), records.get("contract_items.csv"));
		assertEquals(expectedBids(900, List.of(bid)), records.get("contract_bids.csv"));
		assertNoSubDataExceptItemsAndBids();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/bids/900?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/items/900?datasource=tranquility&language=en&page=1",
				"/meta_groups/15",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * No previous archive. Single auction contract with two bids on two separate pages. Both pages
	 * should be fetched and both bids should appear in the archive.
	 */
	@Test
	@SneakyThrows
	void paginatedBidsNoExistingArchive() {
		var bid1 = bid(950001);
		var bid2 = bid(950002);
		var contract = contract(950).put("type", "auction");
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contract)))
				.withItems(950, itemsJson(List.of()))
				.withBids(950, 1, bidsJson(List.of(bid1)))
				.withBids(950, 2, bidsJson(List.of(bid2)))
				.withMetaGroups(NON_ABYSSAL_META_GROUPS_JSON));
		run();

		assertEquals(expectedContracts(List.of(contract), 10000001), records.get("contracts.csv"));
		assertEquals(List.of(), records.get("contract_items.csv"));
		assertEquals(expectedBids(950, List.of(bid1, bid2)), records.get("contract_bids.csv"));
		assertNoSubDataExceptItemsAndBids();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/bids/950?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/bids/950?datasource=tranquility&language=en&page=2",
				"/latest/contracts/public/items/950?datasource=tranquility&language=en&page=1",
				"/meta_groups/15",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * Existing archive has two item_exchange contracts, each with items. The ESI only returns one of
	 * them. The missing item_exchange contract should be removed from the new archive along with its
	 * items.
	 */
	@Test
	@SneakyThrows
	void missingItemExchangeContractItemsRemovedFromArchive() {
		var itemA = item(1000001, 34);
		var itemB = item(1001001, 35);
		var contractA = contract(1000).put("type", "item_exchange");
		var contractB = contract(1001).put("type", "item_exchange");

		var existingContracts = sortedByContractId(
				expectedContracts(List.of(contractA), 10000001), expectedContracts(List.of(contractB), 10000001));
		var existingItems = sortedByRecordId(expectedItems(1000, List.of(itemA)), expectedItems(1001, List.of(itemB)));
		var existingArchive = createExistingArchive(existingContracts, existingItems);

		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contractA)))
				.withLatestArchive(existingArchive));
		run();

		assertEquals(expectedContracts(List.of(contractA), 10000001), records.get("contracts.csv"));
		assertEquals(expectedItems(1000, List.of(itemA)), records.get("contract_items.csv"));
		assertNoSubDataExceptItems();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * Existing archive has an item_exchange contract with items. The ESI returns that same contract
	 * plus a new item_exchange contract. Items for the existing contract should not be re-fetched;
	 * items for the new contract should be fetched.
	 */
	@Test
	@SneakyThrows
	void existingItemExchangeItemsNotRefetched() {
		var itemA = item(1100001, 34);
		var itemC = item(1102001, 36);
		var contractA = contract(1100).put("type", "item_exchange");
		var contractC = contract(1102).put("type", "item_exchange");

		var existingContracts = expectedContracts(List.of(contractA), 10000001);
		var existingItems = expectedItems(1100, List.of(itemA));
		var existingArchive = createExistingArchive(existingContracts, existingItems);

		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contractA, contractC)))
				.withItems(1102, itemsJson(List.of(itemC)))
				.withLatestArchive(existingArchive)
				.withMetaGroups(NON_ABYSSAL_META_GROUPS_JSON));
		run();

		assertEquals(
				sortedByContractId(
						expectedContracts(List.of(contractA), 10000001),
						expectedContracts(List.of(contractC), 10000001)),
				records.get("contracts.csv"));
		assertEquals(
				sortedByRecordId(expectedItems(1100, List.of(itemA)), expectedItems(1102, List.of(itemC))),
				records.get("contract_items.csv"));
		assertNoSubDataExceptItems();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/items/1102?datasource=tranquility&language=en&page=1",
				"/meta_groups/15",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * Existing archive has two auction contracts, each with items and bids. The ESI only returns one
	 * of them. The missing auction contract should be removed along with both its items and bids.
	 */
	@Test
	@SneakyThrows
	void missingAuctionContractItemsAndBidsRemovedFromArchive() {
		var itemA = item(1200001, 34);
		var itemB = item(1201001, 35);
		var bidA = bid(1200901);
		var bidB = bid(1201901);
		var contractA = contract(1200).put("type", "auction");
		var contractB = contract(1201).put("type", "auction");

		var existingContracts = sortedByContractId(
				expectedContracts(List.of(contractA), 10000001), expectedContracts(List.of(contractB), 10000001));
		var existingItems = sortedByRecordId(expectedItems(1200, List.of(itemA)), expectedItems(1201, List.of(itemB)));
		var existingBids = sortedByBidId(expectedBids(1200, List.of(bidA)), expectedBids(1201, List.of(bidB)));
		var existingArchive = createExistingArchive(existingContracts, existingItems, existingBids);

		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contractA)))
				.withBids(1200, bidsJson(List.of(bidA)))
				.withLatestArchive(existingArchive));
		run();

		assertEquals(expectedContracts(List.of(contractA), 10000001), records.get("contracts.csv"));
		assertEquals(expectedItems(1200, List.of(itemA)), records.get("contract_items.csv"));
		assertEquals(expectedBids(1200, List.of(bidA)), records.get("contract_bids.csv"));
		assertNoSubDataExceptItemsAndBids();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/bids/1200?datasource=tranquility&language=en&page=1",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * Existing archive has an auction contract with items and a bid. The ESI returns that same
	 * contract. Bids are always re-fetched from the ESI (unlike items, which are skipped when the
	 * contract already has known items in the archive).
	 */
	@Test
	@SneakyThrows
	void existingAuctionBidsRefetchedItemsNotRefetched() {
		var itemA = item(1300001, 34);
		var bidA = bid(1300901);
		var contractA = contract(1300).put("type", "auction");

		var existingContracts = expectedContracts(List.of(contractA), 10000001);
		var existingItems = expectedItems(1300, List.of(itemA));
		var existingBids = expectedBids(1300, List.of(bidA));
		var existingArchive = createExistingArchive(existingContracts, existingItems, existingBids);

		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contractA)))
				.withBids(1300, bidsJson(List.of(bidA)))
				.withLatestArchive(existingArchive));
		run();

		assertEquals(expectedContracts(List.of(contractA), 10000001), records.get("contracts.csv"));
		assertEquals(expectedItems(1300, List.of(itemA)), records.get("contract_items.csv"));
		assertEquals(expectedBids(1300, List.of(bidA)), records.get("contract_bids.csv"));
		assertNoSubDataExceptItemsAndBids();
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/bids/1300?datasource=tranquility&language=en&page=1",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * No previous archive. One item_exchange contract with one abyssal (dynamic) item.
	 * The contract, item, dynamic item attributes, and effects should all appear in the archive.
	 */
	@Test
	@SneakyThrows
	void singleDynamicItemNoExistingArchive() {
		var typeId = 47804;
		var item = abyssalItem(1400001, 1400001, typeId);
		var contract = contract(1400).put("type", "item_exchange");
		var metaGroupsJson = "{\"meta_group_id\":15,\"type_ids\":[" + typeId + "]}";

		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withContracts(10000001, contractsJson(List.of(contract)))
				.withItems(1400, itemsJson(List.of(item)))
				.withDynamicItems(typeId, 1400001, dynamicItemJson())
				.withType(typeId)
				.withMetaGroups(metaGroupsJson));
		run();

		assertEquals(expectedContracts(List.of(contract), 10000001), records.get("contracts.csv"));
		assertEquals(expectedItems(1400, List.of(item)), records.get("contract_items.csv"));
		assertEquals(expectedDynamicItems(1400, 1400001), records.get("contract_dynamic_items.csv"));
		assertEquals(
				expectedDynamicAttributes(1400, 1400001), records.get("contract_dynamic_items_dogma_attributes.csv"));
		assertEquals(expectedDynamicEffects(1400, 1400001), records.get("contract_dynamic_items_dogma_effects.csv"));
		assertEquals(List.of(), records.get("contract_non_dynamic_items.csv"));
		assertEquals(List.of(), records.get("contract_bids.csv"));
		assertLatestFileMatches();
		assertRequestPaths(
				"/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
				"/latest/contracts/public/items/1400?datasource=tranquility&language=en&page=1",
				"/latest/dogma/dynamic/items/47804/1400001/?datasource=tranquility&language=en",
				"/meta_groups/15",
				"/public-contracts/public-contracts-latest.v2.tar.bz2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility",
				"/universe/types/47804/?datasource=tranquility");
		assertDataIndex();
	}

	// --- Run and capture ---

	@SneakyThrows
	private void run() {
		scrapePublicContracts
				.setScrapeTime(ZonedDateTime.parse("2020-02-03T04:05:06.89Z"))
				.run();
		content = mockS3Adapter
				.getTestObject(BUCKET_NAME, ARCHIVE_FILE, dataClient)
				.orElseThrow();
		records = testDataUtil.readFileMapsFromBz2TarCsv(content);
		var raw = new ArrayList<RecordedRequest>();
		RecordedRequest req;
		while ((req = server.takeRequest(1, TimeUnit.MILLISECONDS)) != null) raw.add(req);
		requestPaths =
				raw.stream().map(RecordedRequest::getPath).sorted().distinct().toList();
	}

	// --- Assertion helpers ---

	private void assertNoSubData() {
		assertEquals(List.of(), records.get("contract_bids.csv"));
		assertEquals(List.of(), records.get("contract_items.csv"));
		assertEquals(List.of(), records.get("contract_dynamic_items.csv"));
		assertEquals(List.of(), records.get("contract_non_dynamic_items.csv"));
		assertEquals(List.of(), records.get("contract_dynamic_items_dogma_attributes.csv"));
		assertEquals(List.of(), records.get("contract_dynamic_items_dogma_effects.csv"));
	}

	private void assertNoSubDataExceptItems() {
		assertEquals(List.of(), records.get("contract_bids.csv"));
		assertEquals(List.of(), records.get("contract_dynamic_items.csv"));
		assertEquals(List.of(), records.get("contract_non_dynamic_items.csv"));
		assertEquals(List.of(), records.get("contract_dynamic_items_dogma_attributes.csv"));
		assertEquals(List.of(), records.get("contract_dynamic_items_dogma_effects.csv"));
	}

	private void assertNoSubDataExceptItemsAndBids() {
		assertEquals(List.of(), records.get("contract_dynamic_items.csv"));
		assertEquals(List.of(), records.get("contract_non_dynamic_items.csv"));
		assertEquals(List.of(), records.get("contract_dynamic_items_dogma_attributes.csv"));
		assertEquals(List.of(), records.get("contract_dynamic_items_dogma_effects.csv"));
	}

	private void assertLatestFileMatches() {
		mockS3Adapter.assertSameContent(BUCKET_NAME, ARCHIVE_FILE, LATEST_FILE, dataClient);
	}

	private void assertRequestPaths(String... paths) {
		assertEquals(List.of(paths), requestPaths);
	}

	private void assertDataIndex() {
		Mockito.verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder().bucket(BUCKET_NAME).path(LATEST_FILE).build(),
						S3Url.builder().bucket(BUCKET_NAME).path(ARCHIVE_FILE).build());
	}

	// --- Archive builders ---

	@SneakyThrows
	private byte[] createExistingArchive(List<Map<String, String>> contracts) {
		return createExistingArchive(contracts, List.of());
	}

	@SneakyThrows
	private byte[] createExistingArchive(List<Map<String, String>> contracts, List<Map<String, String>> items) {
		return createExistingArchive(contracts, items, List.of());
	}

	@SneakyThrows
	private byte[] createExistingArchive(
			List<Map<String, String>> contracts, List<Map<String, String>> items, List<Map<String, String>> bids) {
		var meta = new ContractsScrapeMeta();
		meta.setDatasource("tranquility");
		meta.setScrapeStart(Instant.parse("2020-01-01T00:00:00Z"));
		meta.setScrapeEnd(Instant.parse("2020-01-01T00:01:00Z"));
		return testDataUtil.createBz2Tar(Map.of(
				"contracts.csv",
				writeCsv(contracts),
				"contract_bids.csv",
				writeCsv(bids),
				"contract_items.csv",
				writeCsv(items),
				"contract_dynamic_items.csv",
				new byte[0],
				"contract_non_dynamic_items.csv",
				new byte[0],
				"contract_dynamic_items_dogma_attributes.csv",
				new byte[0],
				"contract_dynamic_items_dogma_effects.csv",
				new byte[0],
				"meta.json",
				objectMapper.writeValueAsBytes(meta)));
	}

	private byte[] writeCsv(List<Map<String, String>> rows) {
		if (rows.isEmpty()) return new byte[0];
		var headers = new ArrayList<>(rows.get(0).keySet());
		var sb = new StringBuilder();
		sb.append(String.join(",", headers)).append("\r\n");
		for (var row : rows) {
			var values = headers.stream()
					.map(h -> {
						var v = row.getOrDefault(h, "");
						if (v.contains(",") || v.contains("\"") || v.contains("\r") || v.contains("\n")) {
							v = "\"" + v.replace("\"", "\"\"") + "\"";
						}
						return v;
					})
					.toList();
			sb.append(String.join(",", values)).append("\r\n");
		}
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	// --- Contract builders ---

	/**
	 * Creates a default courier contract ObjectNode with the given contract ID.
	 * Callers can override individual fields via {@link ObjectNode#put} before use.
	 */
	private ObjectNode contract(long contractId) {
		return objectMapper
				.createObjectNode()
				.put("contract_id", contractId)
				.put("type", "courier")
				.put("collateral", 500000000)
				.put("price", 0)
				.put("reward", 1000000)
				.put("volume", 100.5)
				.put("date_issued", "2023-04-03T05:48:04Z")
				.put("date_expired", "2023-05-01T05:48:04Z")
				.put("days_to_complete", 3)
				.put("end_location_id", 60003760L)
				.put("issuer_corporation_id", 1000001)
				.put("issuer_id", 2000001L)
				.put("start_location_id", 60012547L)
				.put("title", "");
	}

	private ObjectNode item(long recordId, int typeId) {
		return objectMapper
				.createObjectNode()
				.put("is_included", true)
				.put("quantity", 1)
				.put("record_id", recordId)
				.put("type_id", typeId);
	}

	private ObjectNode bid(long bidId) {
		return objectMapper
				.createObjectNode()
				.put("amount", 1000000)
				.put("bid_id", bidId)
				.put("date_bid", "2023-04-03T05:48:04Z");
	}

	@SneakyThrows
	private String bidsJson(List<ObjectNode> bids) {
		var array = objectMapper.createArrayNode();
		bids.forEach(array::add);
		return objectMapper.writeValueAsString(array);
	}

	@SneakyThrows
	private List<Map<String, String>> expectedBids(long contractId, List<ObjectNode> bids) {
		var array = objectMapper.createArrayNode();
		bids.forEach(array::add);
		var maps = testDataUtil.readMapsFromJson(new ByteArrayInputStream(objectMapper.writeValueAsBytes(array)));
		maps.forEach(m -> {
			m.put("contract_id", String.valueOf(contractId));
			m.put("http_last_modified", lastModifiedInstant.toString());
		});
		maps.sort(Comparator.comparingLong(m -> Long.parseLong(m.get("bid_id"))));
		return maps;
	}

	@SneakyThrows
	private String itemsJson(List<ObjectNode> items) {
		var array = objectMapper.createArrayNode();
		items.forEach(array::add);
		return objectMapper.writeValueAsString(array);
	}

	@SneakyThrows
	private List<Map<String, String>> expectedItems(long contractId, List<ObjectNode> items) {
		var array = objectMapper.createArrayNode();
		items.forEach(array::add);
		var maps = testDataUtil.readMapsFromJson(new ByteArrayInputStream(objectMapper.writeValueAsBytes(array)));
		maps.forEach(m -> {
			m.put("contract_id", String.valueOf(contractId));
			m.put("http_last_modified", lastModifiedInstant.toString());
		});
		maps.sort(Comparator.comparingLong(m -> Long.parseLong(m.get("record_id"))));
		return maps;
	}

	@SneakyThrows
	private String contractsJson(List<ObjectNode> contracts) {
		var array = objectMapper.createArrayNode();
		contracts.forEach(array::add);
		return objectMapper.writeValueAsString(array);
	}

	@SafeVarargs
	private List<Map<String, String>> sortedByContractId(List<Map<String, String>>... lists) {
		var result = new ArrayList<Map<String, String>>();
		for (var list : lists) result.addAll(list);
		result.sort(Comparator.comparingLong(m -> Long.parseLong(m.get("contract_id"))));
		return result;
	}

	@SafeVarargs
	private List<Map<String, String>> sortedByRecordId(List<Map<String, String>>... lists) {
		var result = new ArrayList<Map<String, String>>();
		for (var list : lists) result.addAll(list);
		result.sort(Comparator.comparingLong(m -> Long.parseLong(m.get("record_id"))));
		return result;
	}

	@SafeVarargs
	private List<Map<String, String>> sortedByBidId(List<Map<String, String>>... lists) {
		var result = new ArrayList<Map<String, String>>();
		for (var list : lists) result.addAll(list);
		result.sort(Comparator.comparingLong(m -> Long.parseLong(m.get("bid_id"))));
		return result;
	}

	@SneakyThrows
	private List<Map<String, String>> expectedContracts(List<ObjectNode> contracts, long regionId) {
		var array = objectMapper.createArrayNode();
		contracts.forEach(array::add);
		var maps = testDataUtil.readMapsFromJson(new ByteArrayInputStream(objectMapper.writeValueAsBytes(array)));
		maps.forEach(m -> {
			m.put("region_id", String.valueOf(regionId));
			m.put("constellation_id", "999");
			m.put("system_id", "999");
			m.put("station_id", "999");
			m.put("http_last_modified", lastModifiedInstant.toString());
		});
		maps.sort(Comparator.comparingLong(m -> Long.parseLong(m.get("contract_id"))));
		return maps;
	}

	private ObjectNode abyssalItem(long recordId, long itemId, int typeId) {
		return item(recordId, typeId).put("item_id", itemId);
	}

	@SneakyThrows
	private String dynamicItemJson() {
		var node = objectMapper
				.createObjectNode()
				.put("created_by", 203457312)
				.put("mutator_type_id", 47801)
				.put("source_type_id", 31928);
		node.set(
				"dogma_attributes",
				objectMapper
						.createArrayNode()
						.add(objectMapper
								.createObjectNode()
								.put("attribute_id", 277)
								.put("value", 1)));
		node.set(
				"dogma_effects",
				objectMapper
						.createArrayNode()
						.add(objectMapper
								.createObjectNode()
								.put("effect_id", 16)
								.put("is_default", false)));
		return objectMapper.writeValueAsString(node);
	}

	private List<Map<String, String>> expectedDynamicItems(long contractId, long itemId) {
		var map = new HashMap<String, String>();
		map.put("created_by", "203457312");
		map.put("mutator_type_id", "47801");
		map.put("source_type_id", "31928");
		map.put("item_id", String.valueOf(itemId));
		map.put("contract_id", String.valueOf(contractId));
		map.put("http_last_modified", lastModifiedInstant.toString());
		return List.of(map);
	}

	private List<Map<String, String>> expectedDynamicAttributes(long contractId, long itemId) {
		var map = new HashMap<String, String>();
		map.put("attribute_id", "277");
		map.put("value", "1");
		map.put("contract_id", String.valueOf(contractId));
		map.put("item_id", String.valueOf(itemId));
		map.put("http_last_modified", lastModifiedInstant.toString());
		return List.of(map);
	}

	private List<Map<String, String>> expectedDynamicEffects(long contractId, long itemId) {
		var map = new HashMap<String, String>();
		map.put("effect_id", "16");
		map.put("is_default", "false");
		map.put("contract_id", String.valueOf(contractId));
		map.put("item_id", String.valueOf(itemId));
		map.put("http_last_modified", lastModifiedInstant.toString());
		return List.of(map);
	}

	// --- Dispatcher builder ---

	private TestDispatcher dispatcher() {
		return new TestDispatcher();
	}

	class TestDispatcher extends Dispatcher {
		private static final Map<Long, String> REGION_NAMES = Map.of(10000001L, "Derelik", 10000002L, "The Forge");

		private final List<Long> regionIds = new ArrayList<>();
		// key: regionId/contractId, value: map of page -> JSON body (-1 = wildcard for all pages)
		private final Map<Long, Map<Integer, String>> contractsByRegionPage = new HashMap<>();
		private final Map<Long, Map<Integer, String>> itemsByContractPage = new HashMap<>();
		private final Map<Long, Map<Integer, String>> bidsByContractPage = new HashMap<>();
		// key: "typeId-itemId"
		private final Map<String, String> dynamicItemsByKey = new HashMap<>();
		private final Set<Integer> knownTypeIds = new HashSet<>();
		private String metaGroupsBody;
		private Supplier<MockResponse> latestArchiveSupplier = () -> new MockResponse().setResponseCode(404);

		TestDispatcher withRegion(long id) {
			regionIds.add(id);
			return this;
		}

		TestDispatcher withContracts(long regionId, String jsonBody) {
			contractsByRegionPage
					.computeIfAbsent(regionId, k -> new HashMap<>())
					.put(-1, jsonBody);
			return this;
		}

		TestDispatcher withContracts(long regionId, int page, String jsonBody) {
			contractsByRegionPage
					.computeIfAbsent(regionId, k -> new HashMap<>())
					.put(page, jsonBody);
			return this;
		}

		TestDispatcher withItems(long contractId, String jsonBody) {
			itemsByContractPage
					.computeIfAbsent(contractId, k -> new HashMap<>())
					.put(-1, jsonBody);
			return this;
		}

		TestDispatcher withItems(long contractId, int page, String jsonBody) {
			itemsByContractPage
					.computeIfAbsent(contractId, k -> new HashMap<>())
					.put(page, jsonBody);
			return this;
		}

		TestDispatcher withBids(long contractId, String jsonBody) {
			bidsByContractPage.computeIfAbsent(contractId, k -> new HashMap<>()).put(-1, jsonBody);
			return this;
		}

		TestDispatcher withBids(long contractId, int page, String jsonBody) {
			bidsByContractPage.computeIfAbsent(contractId, k -> new HashMap<>()).put(page, jsonBody);
			return this;
		}

		TestDispatcher withDynamicItems(long typeId, long itemId, String jsonBody) {
			dynamicItemsByKey.put(typeId + "-" + itemId, jsonBody);
			return this;
		}

		TestDispatcher withType(int typeId) {
			knownTypeIds.add(typeId);
			return this;
		}

		TestDispatcher withMetaGroups(String jsonBody) {
			metaGroupsBody = jsonBody;
			return this;
		}

		TestDispatcher withLatestArchive(byte[] data) {
			latestArchiveSupplier =
					() -> new MockResponse().setResponseCode(200).setBody(new Buffer().write(data));
			return this;
		}

		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) {
			try {
				var path = request.getRequestUrl().encodedPath();
				var segments = request.getRequestUrl().pathSegments();
				var page = request.getRequestUrl().queryParameter("page");
				var pageNum = page != null ? Integer.parseInt(page) : 1;

				switch (path) {
					case "/universe/regions/", "/latest/universe/regions/":
						return mockJson(regionIds.toString());
					case "/public-contracts/public-contracts-latest.v2.tar.bz2":
						return latestArchiveSupplier.get();
					case "/meta_groups/15":
						return metaGroupsBody != null
								? mockJson(metaGroupsBody)
								: new MockResponse().setResponseCode(404);
				}
				if (path.startsWith("/universe/regions/") || path.startsWith("/latest/universe/regions/")) {
					var segmentIndex = segments.contains("latest") ? 3 : 2;
					var regionId = Long.parseLong(segments.get(segmentIndex));
					var name = REGION_NAMES.getOrDefault(regionId, "Region " + regionId);
					return mockJson("{\"region_id\":" + regionId + ",\"name\":\"" + name + "\",\"constellations\":[]}");
				}
				if (path.startsWith("/contracts/public/items/") || path.startsWith("/latest/contracts/public/items/")) {
					var segmentIndex = segments.contains("latest") ? 4 : 3;
					var contractId = Long.parseLong(segments.get(segmentIndex));
					var pages = itemsByContractPage.get(contractId);
					if (pages == null) return new MockResponse().setResponseCode(404);
					var body = pages.containsKey(pageNum) ? pages.get(pageNum) : pages.get(-1);
					if (body == null) return new MockResponse().setResponseCode(404);
					var totalPages = pages.containsKey(-1) ? 1 : pages.size();
					return mockJson(body).addHeader("x-pages", String.valueOf(totalPages));
				}
				if (path.startsWith("/contracts/public/bids/") || path.startsWith("/latest/contracts/public/bids/")) {
					var segmentIndex = segments.contains("latest") ? 4 : 3;
					var contractId = Long.parseLong(segments.get(segmentIndex));
					var pages = bidsByContractPage.get(contractId);
					if (pages == null) return new MockResponse().setResponseCode(404);
					var body = pages.containsKey(pageNum) ? pages.get(pageNum) : pages.get(-1);
					if (body == null) return new MockResponse().setResponseCode(404);
					var totalPages = pages.containsKey(-1) ? 1 : pages.size();
					return mockJson(body).addHeader("x-pages", String.valueOf(totalPages));
				}
				if (path.startsWith("/contracts/public/") || path.startsWith("/latest/contracts/public/")) {
					var segmentIndex = segments.contains("latest") ? 3 : 2;
					var regionId = Long.parseLong(segments.get(segmentIndex));
					var pages = contractsByRegionPage.get(regionId);
					if (pages == null) return new MockResponse().setResponseCode(404);
					var body = pages.containsKey(pageNum) ? pages.get(pageNum) : pages.get(-1);
					if (body == null) return new MockResponse().setResponseCode(404);
					var totalPages = pages.containsKey(-1) ? 1 : pages.size();
					return mockJson(body).addHeader("x-pages", String.valueOf(totalPages));
				}
				if (path.startsWith("/dogma/dynamic/items/") || path.startsWith("/latest/dogma/dynamic/items/")) {
					var segmentIndex = segments.contains("latest") ? 4 : 3;
					var typeId = Long.parseLong(segments.get(segmentIndex));
					var itemId = Long.parseLong(segments.get(segmentIndex + 1));
					var body = dynamicItemsByKey.get(typeId + "-" + itemId);
					return body != null ? mockJson(body) : new MockResponse().setResponseCode(404);
				}
				if (path.startsWith("/universe/types/")) {
					var typeId = Integer.parseInt(segments.get(2));
					if (knownTypeIds.contains(typeId)) {
						return mockJson("{\"type_id\":" + typeId
								+ ",\"name\":\"Type " + typeId
								+ "\",\"description\":\"\",\"group_id\":1,\"published\":true}");
					}
					return new MockResponse().setResponseCode(404);
				}

				log.error("Unaccounted URL: {}", path);
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	// --- HTTP helpers ---

	@NotNull
	private MockResponse mockJson(String body) {
		return new MockResponse().setResponseCode(200).setBody(body).addHeader("last-modified", lastModified);
	}
}
