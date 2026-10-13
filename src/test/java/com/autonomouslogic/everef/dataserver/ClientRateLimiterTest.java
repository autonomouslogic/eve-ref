package com.autonomouslogic.everef.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientRateLimiterTest {
	private static final String IP = "203.0.113.1";

	private long now;
	private final TimeMeter clock = new TimeMeter() {
		@Override
		public long currentTimeNanos() {
			return now;
		}

		@Override
		public boolean isWallClockBased() {
			return false;
		}
	};

	@BeforeEach
	void setup() {
		now = 0;
	}

	private ClientRateLimiter limiter(boolean enabled, int burst, double perSecond, int maxClients, String allowlist) {
		return new ClientRateLimiter(enabled, burst, perSecond, maxClients, allowlist, clock);
	}

	private void advance(Duration duration) {
		now += duration.toNanos();
	}

	@Test
	void shouldAllowBurstThenReject() {
		var limiter = limiter(true, 3, 1, 100, "");
		for (int i = 0; i < 3; i++) {
			assertTrue(limiter.tryConsume(IP).allowed());
		}
		assertFalse(limiter.tryConsume(IP).allowed());
	}

	@Test
	void shouldReportRetryAfterRoundedUp() {
		var limiter = limiter(true, 1, 0.5, 100, "");
		assertTrue(limiter.tryConsume(IP).allowed());
		assertEquals(2, limiter.tryConsume(IP).retryAfterSeconds());
		advance(Duration.ofMillis(500));
		assertEquals(2, limiter.tryConsume(IP).retryAfterSeconds());
		advance(Duration.ofMillis(600));
		assertEquals(1, limiter.tryConsume(IP).retryAfterSeconds());
	}

	@Test
	void shouldHaveMinimumRetryAfterOfOneSecond() {
		var limiter = limiter(true, 1, 100, 100, "");
		assertTrue(limiter.tryConsume(IP).allowed());
		assertEquals(1, limiter.tryConsume(IP).retryAfterSeconds());
	}

	@Test
	void shouldRefillAfterClockAdvance() {
		var limiter = limiter(true, 2, 1, 100, "");
		assertTrue(limiter.tryConsume(IP).allowed());
		assertTrue(limiter.tryConsume(IP).allowed());
		assertFalse(limiter.tryConsume(IP).allowed());
		advance(Duration.ofSeconds(1));
		assertTrue(limiter.tryConsume(IP).allowed());
		assertFalse(limiter.tryConsume(IP).allowed());
	}

	@Test
	void shouldNotRefillAboveBurst() {
		var limiter = limiter(true, 2, 1, 100, "");
		advance(Duration.ofHours(1));
		assertTrue(limiter.tryConsume(IP).allowed());
		assertTrue(limiter.tryConsume(IP).allowed());
		assertFalse(limiter.tryConsume(IP).allowed());
	}

	@Test
	void shouldKeepIpsIndependent() {
		var limiter = limiter(true, 1, 1, 100, "");
		assertTrue(limiter.tryConsume("203.0.113.1").allowed());
		assertFalse(limiter.tryConsume("203.0.113.1").allowed());
		assertTrue(limiter.tryConsume("203.0.113.2").allowed());
	}

	@Test
	void shouldShareBucketAcrossAnIpv6Slash64() {
		var limiter = limiter(true, 1, 1, 100, "");
		assertTrue(limiter.tryConsume("2001:db8:1:2::1").allowed());
		assertFalse(limiter.tryConsume("2001:db8:1:2:ffff::9").allowed());
		assertTrue(limiter.tryConsume("2001:db8:1:3::1").allowed());
	}

	@Test
	void shouldNeverLimitAllowlistedIps() {
		var limiter = limiter(true, 1, 1, 100, " 203.0.113.1 , 2001:db8:1:2::1");
		for (int i = 0; i < 10; i++) {
			assertTrue(limiter.tryConsume("203.0.113.1").allowed());
			assertTrue(limiter.tryConsume("2001:db8:1:2::77").allowed());
		}
		assertTrue(limiter.tryConsume("203.0.113.2").allowed());
		assertFalse(limiter.tryConsume("203.0.113.2").allowed());
	}

	@Test
	void shouldNeverLimitWhenDisabled() {
		var limiter = limiter(false, 1, 1, 100, "");
		for (int i = 0; i < 10; i++) {
			assertTrue(limiter.tryConsume(IP).allowed());
		}
	}

	@Test
	void shouldForgetIdleClients() {
		var limiter = limiter(true, 2, 1, 100, "");
		assertTrue(limiter.tryConsume(IP).allowed());
		assertTrue(limiter.tryConsume(IP).allowed());
		assertFalse(limiter.tryConsume(IP).allowed());
		// Past the idle expiry the bucket is gone, which is the same as a full bucket after refill.
		advance(Duration.ofMinutes(5));
		assertTrue(limiter.tryConsume(IP).allowed());
		assertTrue(limiter.tryConsume(IP).allowed());
		assertFalse(limiter.tryConsume(IP).allowed());
	}

	@Test
	void shouldBoundTheNumberOfTrackedClients() {
		var limiter = limiter(true, 1, 0.0001, 10, "");
		for (int i = 0; i < 1000; i++) {
			assertTrue(limiter.tryConsume("10.0." + (i / 256) + "." + (i % 256)).allowed());
		}
	}
}
