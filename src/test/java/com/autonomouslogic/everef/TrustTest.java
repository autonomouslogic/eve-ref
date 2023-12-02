package com.autonomouslogic.everef;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Temporary test to play with trust models.
 */
@Log4j2
public class TrustTest {
	TrustHandler trust;

	@BeforeEach
	void setup() {
		trust = new TrustHandler();
	}

	@Test
	void shouldTrustRecordFromImplicitlyTrustedUsers() {
		trust.setUserTrust("A", TrustLevel.IMPLICIT);
		trust.recordSupplied("A", "1");
		assertTrue(trust.recordTrusted("1"));
	}

	@ParameterizedTest
	@EnumSource(
			value = TrustLevel.class,
			names = {"IMPLICIT"},
			mode = EnumSource.Mode.EXCLUDE)
	void shouldNotTrustRecordsFromOtherUsers(TrustLevel level) {
		trust.setUserTrust("A", level);
		trust.recordSupplied("A", "1");
		assertFalse(trust.recordTrusted("1"));
	}

	@Test
	void shouldTrustRecordsTwoHighTrustUsers() {
		trust.setUserTrust("A", TrustLevel.HIGH);
		trust.setUserTrust("B", TrustLevel.HIGH);
		trust.recordSupplied("A", "1");
		assertFalse(trust.recordTrusted("1"));
		trust.recordSupplied("B", "1");
		assertTrue(trust.recordTrusted("1"));
	}

	@ParameterizedTest
	@EnumSource(
			value = TrustLevel.class,
			names = {"IMPLICIT", "HIGH"},
			mode = EnumSource.Mode.EXCLUDE)
	void shouldNotTrustRecordsMultipleOtherUsers(TrustLevel level) {
		trust.setUserTrust("A", level);
		trust.setUserTrust("B", level);
		trust.recordSupplied("A", "1");
		trust.recordSupplied("B", "1");
		assertFalse(trust.recordTrusted("1"));
	}

	@RequiredArgsConstructor
	@Getter
	enum TrustLevel {
		UNTRUSTED(-1),
		NONE(0),
		LOW(1),
		HIGH(2),
		IMPLICIT(3);

		final int level;
	}

	static class TrustHandler {
		public void recordSupplied(String userId, String recordId) {}

		public boolean recordTrusted(String recordId) {
			return false;
		}

		public void setUserTrust(String userId, TrustLevel trustLevel) {}

		public TrustLevel getUserTrust(String userId) {
			return TrustLevel.NONE;
		}
	}
}
