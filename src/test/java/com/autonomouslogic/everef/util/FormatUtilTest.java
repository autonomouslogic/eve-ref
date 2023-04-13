package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FormatUtilTest {
	@Test
	void shouldFormatLongsToHex() {
		assertEquals("00000000", FormatUtil.toHexString(0));
		assertEquals("12345678", FormatUtil.toHexString(0x12345678L));
		assertEquals("fedcba98", FormatUtil.toHexString(0xfedcba98L));
	}
}
