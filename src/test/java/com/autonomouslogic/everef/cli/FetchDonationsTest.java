package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.cli.FetchDonations.DonationEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FetchDonationsTest {
	@Test
	void shouldNotDoAnythingWithNoDonations() {
		// No prior donations
		// No current donations
		assertDonationsFile(List.of());
		assertSummaryFile(List.of());
		assertNoDiscordUpdate();
	}

	@Test
	void shouldUpdateWithFirstDonations() {
		// No prior donations
		// New donations
		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertNoDiscordUpdate();
	}

	@Test
	void shouldUpdateWithSameDonations() {
		// Existing prior donations
		// Same donations on ESI journal
		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertNoDiscordUpdate();
	}

	@Test
	void shouldUpdateWithNewDonations() {
		// Existing prior donations
		// New donations
		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertDiscordUpdate(List.of(new Object()));
	}

	@Test
	void shouldSummariseMultipleDonationsFromTheSameEntity() {
		// Existing prior donations from the same person
		// Multiple new donations from the same person
		assertDonationsFile(List.of(DonationEntry.builder().build()));
		assertSummaryFile(List.of(new Object()));
		assertDiscordUpdate(List.of(new Object()));
	}

	@Test
	void shouldReplaceWeirdCharactersInDonorNames() {}

	@Test
	void shouldNotNotifyDiscordOfDonationsBelowMinimum() {}

	private void assertDonationsFile(List<DonationEntry> entries) {}

	private void assertSummaryFile(List<Object> entries) {}

	private void assertNoDiscordUpdate() {}

	private void assertDiscordUpdate(List<Object> entries) {}
}
