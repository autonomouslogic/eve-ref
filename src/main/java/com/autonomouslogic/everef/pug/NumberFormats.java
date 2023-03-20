package com.autonomouslogic.everef.pug;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Objects;

/**
 *
 */
public class NumberFormats {
	private static final String[] SUFFIXES = new String[] {"", "k", "m", "b", "t"};
	private static final String[] ROMAN = new String[] {"I", "II", "III", "IV", "V"};
	public static final DecimalFormat INTEGER = new DecimalFormat("###,##0");
	public static final DecimalFormat DECIMAL2 = new DecimalFormat("###,##0.00");
	private static final String[] SIZES = new String[] {"bytes", "KiB", "MiB", "GiB", "TiB"};

	public static String formatMoney(double v) {
		int suffix = 0;
		while (v > 1000.0 && suffix < SUFFIXES.length - 1) {
			v /= 1e3;
			suffix++;
		}
		return DECIMAL2.format(v) + SUFFIXES[suffix];
	}

	public static String formatFileSize(long v) {
		double d = (double) v;
		int suffix = 0;
		while (d > 1024.0 && suffix < SIZES.length - 1) {
			d /= 1024.0;
			suffix++;
		}
		if (suffix == 0) {
			return INTEGER.format(v) + " " + SIZES[suffix];
		} else {
			return DECIMAL2.format(d) + " " + SIZES[suffix];
		}
	}

	public String money(double v) {
		return formatMoney(v);
	}

	public String money(BigDecimal v) {
		return formatMoney(Objects.requireNonNull(v, "v").doubleValue());
	}

	public String integer(double v) {
		return INTEGER.format(v);
	}

	public String integer(BigInteger v) {
		return INTEGER.format(v);
	}

	public String integer(BigDecimal v) {
		return INTEGER.format(v);
	}

	public String decimal2(double v) {
		return DECIMAL2.format(v);
	}

	public String decimal2(BigInteger v) {
		return DECIMAL2.format(v);
	}

	public String decimal2(BigDecimal v) {
		return DECIMAL2.format(v);
	}

	public String roman(int v) {
		return ROMAN[v - 1];
	}

	public String intValue(Object obj) {
		return Long.toString(((Number) obj).longValue());
	}

	public String fileSize(long v) {
		return formatFileSize(v);
	}
}
