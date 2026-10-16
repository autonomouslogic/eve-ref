package com.autonomouslogic.everef.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ClientIpsTest {
	@Test
	void shouldPreferCfConnectingIp() {
		assertEquals("203.0.113.1", ClientIps.resolve(" 203.0.113.1 ", "198.51.100.1", "10.0.0.1"));
	}

	@Test
	void shouldUseFirstValueOfForwardedFor() {
		assertEquals("198.51.100.1", ClientIps.resolve(null, "198.51.100.1, 10.0.0.2", "10.0.0.1"));
	}

	@Test
	void shouldFallBackToRemoteAddress() {
		assertEquals("10.0.0.1", ClientIps.resolve(null, null, "10.0.0.1"));
		assertEquals("10.0.0.1", ClientIps.resolve("", "  ", "10.0.0.1"));
	}

	@Test
	void shouldSkipValuesThatAreNotIpLiterals() {
		assertEquals("198.51.100.1", ClientIps.resolve("example.com", "198.51.100.1", "10.0.0.1"));
		assertEquals("10.0.0.1", ClientIps.resolve("not-an-ip", "also-not", "10.0.0.1"));
	}

	@Test
	void shouldAcceptIpv6() {
		assertEquals("2001:db8::1", ClientIps.resolve("2001:db8::1", null, "10.0.0.1"));
	}
}
