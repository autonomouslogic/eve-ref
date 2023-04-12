package com.autonomouslogic.everef.util;

import org.apache.commons.lang3.StringUtils;

public class FormatUtil {
	public static String toHexString(long l) {
		return StringUtils.leftPad(Long.toHexString(l), 8, '0');
	}
}
