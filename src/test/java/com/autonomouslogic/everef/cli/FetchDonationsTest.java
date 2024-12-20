package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.everef.cli.FetchDonations.DonationEntry;
import com.autonomouslogic.everef.openapi.esi.models.GetCharactersCharacterIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetCharactersCharacterIdWalletJournal200Ok;
import com.autonomouslogic.everef.openapi.esi.models.GetCorporationsCorporationIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetCorporationsCorporationIdWalletsDivisionJournal200Ok;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
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
	private static final long TEST_DONOR_CHARACTER_ID = 100000001;
	private static final long TEST_DONOR_CORPORATION_ID = 200000001;
	private static final long TEST_CHARACTER_ID = 100000000;
	private static final long TEST_CORPORATION_ID = 200000000;

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

	OffsetDateTime donationTime = OffsetDateTime.now().minusHours(1);

	@Inject
	protected FetchDonationsTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		assertNull(server.takeRequest(0, TimeUnit.MILLISECONDS));
		server.close();
	}

	@Test
	void shouldNotDoAnythingWithNoDonations() {
		// No prior donations
		// No current donations

		fetchDonations.run().blockingAwait();

		assertDonationsFile(List.of());
		assertSummaryFile(List.of());
		assertNoDiscordUpdate();
	}

	@Test
	void shouldUpdateWithFirstDonations() {
		// No prior donations
		// New donations

		fetchDonations.run().blockingAwait();

		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertNoDiscordUpdate();
	}

	@Test
	void shouldUpdateWithSameDonations() {
		// Existing prior donations
		// Same donations on ESI journal

		fetchDonations.run().blockingAwait();

		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertNoDiscordUpdate();
	}

	@Test
	void shouldUpdateWithNewDonations() {
		// Existing prior donations
		// New donations

		fetchDonations.run().blockingAwait();

		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertDiscordUpdate(List.of(new Object()));
	}

	@Test
	void shouldKeepOldDonations() {
		// Existing prior donations
		// New donations, but missing prior

		fetchDonations.run().blockingAwait();

		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertDiscordUpdate(List.of(new Object()));
	}

	@Test
	void shouldSummariseMultipleDonationsFromTheSameEntity() {
		// Existing prior donations from the same person
		// Multiple new donations from the same person

		fetchDonations.run().blockingAwait();

		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertDiscordUpdate(List.of(new Object()));
	}

	@Test
	void shouldReplaceWeirdCharactersInDonorNames() {}

	@Test
	@Disabled
	void shouldNotNotifyDiscordOfDonationsBelowMinimum() {}

	private void assertDonationsFile(List<DonationEntry> entries) {}

	private void assertSummaryFile(List<Object> entries) {}

	private void assertNoDiscordUpdate() {}

	private void assertDiscordUpdate(List<Object> entries) {}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var segments = request.getRequestUrl().pathSegments();

				if (path.equals("/characters/" + TEST_CHARACTER_ID + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(new GetCharactersCharacterIdOk(
									OffsetDateTime.now(),
									0,
									(int) TEST_CORPORATION_ID,
									GetCharactersCharacterIdOk.Gender.female,
									"Test Character",
									0,
									0,
									"",
									0,
									0f,
									"")));
				}

				if (path.equals("/corporations/" + TEST_CORPORATION_ID + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(new GetCorporationsCorporationIdOk(
									0,
									(int) TEST_CHARACTER_ID,
									1,
									"Test Corporation",
									0,
									"T",
									0,
									OffsetDateTime.now(),
									"",
									0,
									0,
									0L,
									"",
									false)));
				}

				if (path.equals("/characters/" + TEST_DONOR_CHARACTER_ID + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(new GetCharactersCharacterIdOk(
									OffsetDateTime.now(),
									0,
									(int) TEST_DONOR_CORPORATION_ID,
									GetCharactersCharacterIdOk.Gender.female,
									"Donor Character",
									0,
									0,
									"",
									0,
									0f,
									"")));
				}

				if (path.equals("/corporations/" + TEST_DONOR_CORPORATION_ID + "/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(new GetCorporationsCorporationIdOk(
									0,
									(int) TEST_DONOR_CORPORATION_ID,
									1,
									"Donor Corporation",
									0,
									"T",
									0,
									OffsetDateTime.now(),
									"",
									0,
									0,
									0L,
									"",
									false)));
				}

				if (path.equals("/characters/1000000000/wallet/journal/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									List.of(new GetCharactersCharacterIdWalletJournal200Ok(
											donationTime,
											"",
											1,
											GetCharactersCharacterIdWalletJournal200Ok.RefType.player_donation,
											100.0,
											0.0,
											0L,
											GetCharactersCharacterIdWalletJournal200Ok.ContextIdType.character_id,
											1000000001,
											"",
											1000000000,
											null,
											null))));
				}

				if (path.equals("/corporations/2000000000/wallets/1/journal/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(
									List.of(new GetCorporationsCorporationIdWalletsDivisionJournal200Ok(
											donationTime,
											"",
											1,
											GetCorporationsCorporationIdWalletsDivisionJournal200Ok.RefType
													.corporation_account_withdrawal,
											100.0,
											0.0,
											0L,
											GetCorporationsCorporationIdWalletsDivisionJournal200Ok.ContextIdType
													.corporation_id,
											2000000001,
											"",
											2000000000,
											null,
											null))));
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
