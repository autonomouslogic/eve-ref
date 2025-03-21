package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.everef.cli.FetchDonations.DonationEntry;
import com.autonomouslogic.everef.cli.FetchDonations.SummaryEntry;
import com.autonomouslogic.everef.cli.FetchDonations.SummaryFile;
import com.autonomouslogic.everef.openapi.esi.model.GetCharactersCharacterIdOk;
import com.autonomouslogic.everef.openapi.esi.model.GetCharactersCharacterIdWalletJournal200Ok;
import com.autonomouslogic.everef.openapi.esi.model.GetCorporationsCorporationIdOk;
import com.autonomouslogic.everef.openapi.esi.model.GetCorporationsCorporationIdWalletsDivisionJournal200Ok;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(
		key = "DONATIONS_DISCORD_WEBHOOK_URL",
		value = "http://localhost:" + TestDataUtil.TEST_PORT + "/discord")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "EVE_REF_CHARACTER_OWNER_HASH", value = "ownerhash")
@SetEnvironmentVariable(key = "STATIC_PATH", value = "s3://static/")
public class FetchDonationsTest {
	private static final int TEST_CHARACTER_ID = 1000000000;
	private static final int TEST_CORPORATION_ID = 2000000000;
	private static final int TEST_DONOR_CHARACTER_ID_1 = 1000000001;
	private static final int TEST_DONOR_CHARACTER_ID_2 = 1000000002;
	private static final int TEST_DONOR_CHARACTER_ID_3 = 1000000003;
	private static final int TEST_DONOR_CORPORATION_ID_1 = 2000000001;
	private static final int TEST_DONOR_CORPORATION_ID_2 = 2000000002;
	private static final int TEST_DONOR_CORPORATION_ID_3 = 2000000003;

	@Inject
	FetchDonations fetchDonations;

	@Inject
	@Named("static")
	protected S3AsyncClient s3Client;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	ObjectMapper objectMapper;

	MockWebServer server;

	OffsetDateTime donationTime = OffsetDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS);

	ObjectNode discordCall;

	List<GetCharactersCharacterIdWalletJournal200Ok> characterJournal;
	List<GetCorporationsCorporationIdWalletsDivisionJournal200Ok> corporationJournal;
	List<DonationEntry> existingDonations;

	@Inject
	protected FetchDonationsTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		discordCall = null;
		characterJournal = new ArrayList<>();
		corporationJournal = new ArrayList<>();
		existingDonations = new ArrayList<>();

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	void shouldNotDoAnythingWithNoDonations() {
		// No prior donations
		// No current donations

		fetchDonations.run();

		assertDonationsFile(List.of());
		assertSummaryFile(SummaryFile.builder().top(List.of()).recent(List.of()).build());
		assertNoDiscordUpdate();
	}

	@Test
	void shouldUpdateWithFirstDonations() {
		// No prior donations
		// New donations
		addCharacterTransaction(1, TEST_DONOR_CHARACTER_ID_1, 100_000_000, donationTime);

		fetchDonations.run();

		assertDonationsFile(List.of(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(100_000_000.0)
				.build()));
		var entries = List.of(SummaryEntry.builder()
				.donorName("Donor Character 1")
				.amount(100_000_000.00)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.build());
		assertSummaryFile(SummaryFile.builder().top(entries).recent(entries).build());

		assertDiscordUpdate("**Donor Character 1** donated 100.00m ISK :gift:");
	}

	@Test
	void shouldUpdateWithSameDonations() {
		// Existing prior donations
		existingDonations.add(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(100_000_000.0)
				.build());
		// Same donations on ESI journal
		addCharacterTransaction(1, TEST_DONOR_CHARACTER_ID_1, 100_000_000, donationTime);

		putDonationsFile();
		fetchDonations.run();

		assertDonationsFile(List.of(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(100_000_000.0)
				.build()));

		var entries = List.of(SummaryEntry.builder()
				.donorName("Donor Character")
				.amount(100_000_000.00)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.build());
		assertSummaryFile(SummaryFile.builder().top(entries).recent(entries).build());

		assertNoDiscordUpdate();
	}

	@Test
	void shouldUpdateWithNewDonations() {
		// Existing prior donations
		existingDonations.add(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(100_000_000.0)
				.build());
		// New donations
		addCharacterTransaction(1, TEST_DONOR_CHARACTER_ID_1, 100_000_000, donationTime);
		addCharacterTransaction(2, TEST_DONOR_CHARACTER_ID_2, 200_000_000, donationTime.plusMinutes(1));

		putDonationsFile();
		fetchDonations.run();

		assertDonationsFile(List.of(
				DonationEntry.builder()
						.id(1)
						.date(donationTime.toInstant())
						.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
						.characterId(TEST_DONOR_CHARACTER_ID_1)
						.donorName("Donor Character 1")
						.secondPartyId(TEST_CHARACTER_ID)
						.amount(100_000_000.0)
						.build(),
				DonationEntry.builder()
						.id(2)
						.date(donationTime.plusMinutes(1).toInstant())
						.firstPartyId(TEST_DONOR_CHARACTER_ID_2)
						.characterId(TEST_DONOR_CHARACTER_ID_2)
						.donorName("Donor Character 2")
						.secondPartyId(TEST_CHARACTER_ID)
						.amount(200_000_000.0)
						.build()));

		var entries = List.of(
				SummaryEntry.builder()
						.donorName("Donor Character 2")
						.amount(200_000_000.00)
						.characterId(TEST_DONOR_CHARACTER_ID_2)
						.build(),
				SummaryEntry.builder()
						.donorName("Donor Character 1")
						.amount(100_000_000.00)
						.characterId(TEST_DONOR_CHARACTER_ID_1)
						.build());
		assertSummaryFile(SummaryFile.builder().top(entries).recent(entries).build());

		assertDiscordUpdate("**Donor Character 2** donated 200.00m ISK :thumbsup:");
	}

	@Test
	void shouldKeepOldDonations() {
		// Existing prior donations
		existingDonations.add(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(100_000_000.0)
				.build());
		// New donations, but missing prior
		addCharacterTransaction(2, TEST_DONOR_CHARACTER_ID_2, 200_000_000, donationTime);

		putDonationsFile();
		fetchDonations.run();

		assertDonationsFile(List.of(
				DonationEntry.builder()
						.id(1)
						.date(donationTime.toInstant())
						.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
						.characterId(TEST_DONOR_CHARACTER_ID_1)
						.donorName("Donor Character 1")
						.secondPartyId(TEST_CHARACTER_ID)
						.amount(100_000_000.0)
						.build(),
				DonationEntry.builder()
						.id(2)
						.date(donationTime.toInstant())
						.firstPartyId(TEST_DONOR_CHARACTER_ID_2)
						.characterId(TEST_DONOR_CHARACTER_ID_2)
						.donorName("Donor Character 2")
						.secondPartyId(TEST_CHARACTER_ID)
						.amount(200_000_000.0)
						.build()));

		var entries = List.of(
				SummaryEntry.builder()
						.donorName("Donor Character 2")
						.amount(200_000_000.00)
						.characterId(TEST_DONOR_CHARACTER_ID_2)
						.build(),
				SummaryEntry.builder()
						.donorName("Donor Character 1")
						.amount(100_000_000.00)
						.characterId(TEST_DONOR_CHARACTER_ID_1)
						.build());
		assertSummaryFile(SummaryFile.builder().top(entries).recent(entries).build());

		assertDiscordUpdate("**Donor Character 2** donated 200.00m ISK :thumbsup:");
	}

	@Test
	void shouldKeepOldDonationsWhenNoDonations() {
		// Existing prior donations
		existingDonations.add(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(100_000_000.0)
				.build());
		// No donations on ESI

		putDonationsFile();
		fetchDonations.run();

		assertDonationsFile(List.of(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(100_000_000.0)
				.build()));

		var entries = List.of(SummaryEntry.builder()
				.donorName("Donor Character 1")
				.amount(100_000_000.00)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.build());
		assertSummaryFile(SummaryFile.builder().top(entries).recent(entries).build());

		assertNoDiscordUpdate();
	}

	@Test
	void shouldSummariseMultipleDonationsFromTheSameEntity() {
		// Existing prior donations from the same person
		existingDonations.add(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(100_000_000.0)
				.build());
		// Multiple new donations from the same person
		addCharacterTransaction(2, TEST_DONOR_CHARACTER_ID_1, 200_000_000, donationTime);
		addCharacterTransaction(3, TEST_DONOR_CHARACTER_ID_1, 300_000_000, donationTime);

		putDonationsFile();
		fetchDonations.run();

		var entries = List.of(SummaryEntry.builder()
				.donorName("Donor Character 1")
				.amount(600_000_000.00)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.build());
		assertSummaryFile(SummaryFile.builder().top(entries).recent(entries).build());

		assertDiscordUpdate("**Donor Character 1** donated 500.00m ISK :money_with_wings:");
	}

	@Test
	void shouldSummariseMultipleDonationsFromDifferentEntities() {
		// No prior donations
		// Multiple new donations from different people
		addCharacterTransaction(1, TEST_DONOR_CHARACTER_ID_1, 200_000_000, donationTime);
		addCharacterTransaction(2, TEST_DONOR_CHARACTER_ID_2, 300_000_000, donationTime);

		putDonationsFile();
		fetchDonations.run();

		var entries = List.of(
				SummaryEntry.builder()
						.donorName("Donor Character 2")
						.amount(300_000_000.00)
						.characterId(TEST_DONOR_CHARACTER_ID_2)
						.build(),
				SummaryEntry.builder()
						.donorName("Donor Character 1")
						.amount(200_000_000.00)
						.characterId(TEST_DONOR_CHARACTER_ID_1)
						.build());
		assertSummaryFile(SummaryFile.builder().top(entries).recent(entries).build());

		assertDiscordUpdate("**Donor Character 2** donated 300.00m ISK :atm:\n"
				+ "**Donor Character 1** donated 200.00m ISK :money_mouth:");
	}

	@Test
	void shouldNotSummariseOldDonations() {
		// Existing prior donations
		existingDonations.add(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(10_000_001.0)
				.build());
		existingDonations.add(DonationEntry.builder()
				.id(2)
				.date(donationTime.toInstant().minus(Duration.ofDays(1)))
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(10_000_010.0)
				.build());
		existingDonations.add(DonationEntry.builder()
				.id(3)
				.date(donationTime.toInstant().minus(Duration.ofDays(90)))
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(10_000_100.0)
				.build());

		putDonationsFile();
		fetchDonations.run();

		assertSummaryFile(SummaryFile.builder()
				.top(List.of(SummaryEntry.builder()
						.donorName("Donor Character 1")
						.amount(20_000_011.0)
						.characterId(TEST_DONOR_CHARACTER_ID_1)
						.build()))
				.recent(List.of(SummaryEntry.builder()
						.donorName("Donor Character 1")
						.amount(10_000_001.0)
						.characterId(TEST_DONOR_CHARACTER_ID_1)
						.build()))
				.build());
	}

	@Test
	void shouldNotIncludeSmallDonationsInRecent() {
		// Existing prior donations
		existingDonations.add(DonationEntry.builder()
				.id(1)
				.date(donationTime.toInstant())
				.firstPartyId(TEST_DONOR_CHARACTER_ID_1)
				.characterId(TEST_DONOR_CHARACTER_ID_1)
				.donorName("Donor Character 1")
				.secondPartyId(TEST_CHARACTER_ID)
				.amount(1.0)
				.build());

		putDonationsFile();
		fetchDonations.run();

		assertSummaryFile(SummaryFile.builder()
				.top(List.of(SummaryEntry.builder()
						.donorName("Donor Character 1")
						.amount(1.0)
						.characterId(TEST_DONOR_CHARACTER_ID_1)
						.build()))
				.recent(List.of())
				.build());
	}

	@Test
	void shouldReplaceWeirdCharactersInDonorNames() {
		addCharacterTransaction(1, TEST_DONOR_CHARACTER_ID_3, 200, donationTime);
		fetchDonations.run();
		assertDiscordUpdate("**Weird name??_??** donated 200.00 ISK :trophy:");
	}

	@Test
	void shouldShortenLargeAmountsOfMoney() {
		addCharacterTransaction(1, TEST_DONOR_CHARACTER_ID_1, 12345678912L, donationTime);
		fetchDonations.run();
		assertDiscordUpdate("**Donor Character 1** donated 12.35b ISK :money_mouth:");
	}

	@Test
	void shouldIncludeAllRelevantTransactions() {
		addCharacterTransaction(
				1,
				TEST_DONOR_CHARACTER_ID_1,
				1,
				donationTime,
				GetCharactersCharacterIdWalletJournal200Ok.RefTypeEnum.PLAYER_DONATION);
		addCharacterTransaction(
				2,
				TEST_DONOR_CORPORATION_ID_1,
				10,
				donationTime,
				GetCharactersCharacterIdWalletJournal200Ok.RefTypeEnum.CORPORATION_ACCOUNT_WITHDRAWAL);
		addCorporationTransaction(
				1,
				TEST_DONOR_CHARACTER_ID_2,
				100,
				donationTime,
				GetCorporationsCorporationIdWalletsDivisionJournal200Ok.RefTypeEnum.PLAYER_DONATION);
		addCorporationTransaction(
				2,
				TEST_DONOR_CORPORATION_ID_2,
				1000,
				donationTime,
				GetCorporationsCorporationIdWalletsDivisionJournal200Ok.RefTypeEnum.CORPORATION_ACCOUNT_WITHDRAWAL);
		fetchDonations.run();
		assertDiscordUpdate("**Donor Corporation 2** donated 1,000.00 ISK :gift:\n"
				+ "**Donor Character 2** donated 100.00 ISK :gem:\n"
				+ "**Donor Corporation 1** donated 10.00 ISK :pound:\n"
				+ "**Donor Character 1** donated 1.00 ISK :trophy:");
	}

	@Test
	void shouldNotIncludeIrrelevantTransactions() {
		addCharacterTransaction(
				1,
				TEST_DONOR_CHARACTER_ID_1,
				1,
				donationTime,
				GetCharactersCharacterIdWalletJournal200Ok.RefTypeEnum.AGENT_DONATION);
		addCorporationTransaction(
				1,
				TEST_DONOR_CORPORATION_ID_1,
				100,
				donationTime,
				GetCorporationsCorporationIdWalletsDivisionJournal200Ok.RefTypeEnum.ACCELERATION_GATE_FEE);
		fetchDonations.run();
		assertDonationsFile(List.of());
		assertSummaryFile(SummaryFile.builder().top(List.of()).recent(List.of()).build());
		assertNoDiscordUpdate();
	}

	@Test
	void shouldNotIncludeTransactionsFromSelf() {
		addCharacterTransaction(
				1,
				TEST_CHARACTER_ID,
				1,
				donationTime,
				GetCharactersCharacterIdWalletJournal200Ok.RefTypeEnum.PLAYER_DONATION);
		addCharacterTransaction(
				1,
				TEST_CORPORATION_ID,
				10,
				donationTime,
				GetCharactersCharacterIdWalletJournal200Ok.RefTypeEnum.CORPORATION_ACCOUNT_WITHDRAWAL);
		addCorporationTransaction(
				1,
				TEST_CHARACTER_ID,
				100,
				donationTime,
				GetCorporationsCorporationIdWalletsDivisionJournal200Ok.RefTypeEnum.PLAYER_DONATION);
		addCorporationTransaction(
				1,
				TEST_CORPORATION_ID,
				1000,
				donationTime,
				GetCorporationsCorporationIdWalletsDivisionJournal200Ok.RefTypeEnum.CORPORATION_ACCOUNT_WITHDRAWAL);
		fetchDonations.run();
		assertDonationsFile(List.of());
		assertSummaryFile(SummaryFile.builder().top(List.of()).recent(List.of()).build());
		assertNoDiscordUpdate();
	}

	@Test
	@Disabled
	void shouldNotNotifyDiscordOfDonationsBelowMinimum() {}

	@SuppressWarnings("unchecked")
	private void assertDonationsFile(List<DonationEntry> expected) {
		var supplied = mockS3Adapter
				.getTestObject("static", FetchDonations.DONATIONS_LIST_FILE, s3Client)
				.map(b -> {
					try {
						var type =
								objectMapper.getTypeFactory().constructCollectionType(List.class, DonationEntry.class);
						return (List<DonationEntry>) objectMapper.readValue(b, type);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.orElse(List.of());
		assertEquals(expected, supplied);
	}

	@SuppressWarnings("unchecked")
	private void assertSummaryFile(SummaryFile expected) {
		var supplied = mockS3Adapter
				.getTestObject("static", FetchDonations.DONATIONS_SUMMARY_FILE, s3Client)
				.map(b -> {
					try {
						return objectMapper.readValue(b, SummaryFile.class);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.orElse(null);
		assertEquals(expected, supplied);
	}

	private void assertNoDiscordUpdate() {
		assertNull(discordCall);
	}

	private void assertDiscordUpdate(String message) {
		assertNotNull(discordCall);
		assertEquals(message, discordCall.get("content").asText());
	}

	private void addCharacterTransaction(int id, int donorId, double amount, OffsetDateTime time) {
		addCharacterTransaction(
				id, donorId, amount, time, GetCharactersCharacterIdWalletJournal200Ok.RefTypeEnum.PLAYER_DONATION);
	}

	private void addCharacterTransaction(
			int id,
			int donorId,
			double amount,
			OffsetDateTime time,
			GetCharactersCharacterIdWalletJournal200Ok.RefTypeEnum refType) {
		characterJournal.add(new GetCharactersCharacterIdWalletJournal200Ok()
				.date(time)
				.id((long) id)
				.refType(refType)
				.amount(amount)
				.firstPartyId(donorId)
				.secondPartyId(TEST_CHARACTER_ID));
	}

	private void addCorporationTransaction(int id, int donorId, double amount, OffsetDateTime time) {
		addCorporationTransaction(
				id,
				donorId,
				amount,
				time,
				GetCorporationsCorporationIdWalletsDivisionJournal200Ok.RefTypeEnum.CORPORATION_ACCOUNT_WITHDRAWAL);
	}

	private void addCorporationTransaction(
			int id,
			int donorId,
			double amount,
			OffsetDateTime time,
			GetCorporationsCorporationIdWalletsDivisionJournal200Ok.RefTypeEnum refType) {
		corporationJournal.add(new GetCorporationsCorporationIdWalletsDivisionJournal200Ok()
				.date(time)
				.id((long) id)
				.amount(amount)
				.refType(refType)
				.firstPartyId(donorId)
				.secondPartyId(TEST_CORPORATION_ID));
	}

	@SneakyThrows
	private void putDonationsFile() {
		mockS3Adapter.putTestObject(
				"static",
				FetchDonations.DONATIONS_LIST_FILE,
				objectMapper.writeValueAsString(existingDonations),
				s3Client);
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var segments = request.getRequestUrl().pathSegments();

				if (path.equals("/characters/" + TEST_CHARACTER_ID + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(new GetCharactersCharacterIdOk()
									.corporationId(TEST_CORPORATION_ID)
									.name("Test Character")));
				}

				if (path.equals("/corporations/" + TEST_CORPORATION_ID + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									new GetCorporationsCorporationIdOk().name("Test Corporation")));
				}

				if (path.equals("/characters/" + TEST_DONOR_CHARACTER_ID_1 + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									new GetCharactersCharacterIdOk().name("Donor Character 1")));
				}

				if (path.equals("/characters/" + TEST_DONOR_CHARACTER_ID_2 + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									new GetCharactersCharacterIdOk().name("Donor Character 2")));
				}

				if (path.equals("/characters/" + TEST_DONOR_CHARACTER_ID_3 + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									new GetCharactersCharacterIdOk().name("Weird name!+_&\\")));
				}

				if (path.equals("/corporations/" + TEST_DONOR_CORPORATION_ID_1 + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									new GetCorporationsCorporationIdOk().name("Donor Corporation 1")));
				}

				if (path.equals("/corporations/" + TEST_DONOR_CORPORATION_ID_2 + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									new GetCorporationsCorporationIdOk().name("Donor Corporation 2")));
				}

				if (path.equals("/corporations/" + TEST_DONOR_CORPORATION_ID_3 + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									new GetCorporationsCorporationIdOk().name("Weird name!+_&\\")));
				}

				if (path.equals("/characters/" + TEST_CHARACTER_ID + "/wallet/journal/")) {
					return new MockResponse().setBody(objectMapper.writeValueAsString(characterJournal));
				}

				if (path.equals("/corporations/" + TEST_CORPORATION_ID + "/wallets/1/journal/")) {
					return new MockResponse().setBody(objectMapper.writeValueAsString(corporationJournal));
				}

				if (path.equals("/discord")) {
					discordCall =
							(ObjectNode) objectMapper.readTree(request.getBody().readUtf8());
					return new MockResponse();
				}

				log.error(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
