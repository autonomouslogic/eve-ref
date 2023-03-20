package com.autonomouslogic.everef.pug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class NumberFormatsTest {
	@Test
	public void shouldFormatSize() {
		assertEquals("604.21 KiB", NumberFormats.formatFileSize(618713));
		assertEquals("3.01 GiB", NumberFormats.formatFileSize(3226976451L));
	}
}
