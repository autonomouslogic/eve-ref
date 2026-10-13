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

	@Test
	void shouldKeyIpv6OnItsSlash64() {
		assertEquals(
				ClientIps.rateLimitKey("2001:db8:1:2:aaaa::1"), ClientIps.rateLimitKey("2001:db8:1:2:bbbb:cccc::2"));
		assertEquals("2001:db8:1:2::/64", ClientIps.rateLimitKey("2001:db8:1:2:aaaa::1"));
	}

	@Test
	void shouldNotMergeDifferentIpv6Subnets() {
		assertEquals(
				false, ClientIps.rateLimitKey("2001:db8:1:2::1").equals(ClientIps.rateLimitKey("2001:db8:1:3::1")));
	}

	@Test
	void shouldKeepIpv4AsIs() {
		assertEquals("203.0.113.1", ClientIps.rateLimitKey("203.0.113.1"));
	}
}
