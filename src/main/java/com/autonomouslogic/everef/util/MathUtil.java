package com.autonomouslogic.everef.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;

public class MathUtil {
	public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
	public static final MathContext MATH_CONTEXT =
			new MathContext(MathContext.DECIMAL128.getPrecision(), ROUNDING_MODE);

	public static BigDecimal divide(BigDecimal a, double b) {
		return divide(a, BigDecimal.valueOf(b));
	}

	public static BigDecimal divide(BigDecimal a, int b) {
		return divide(a, BigDecimal.valueOf(b));
	}

	private static BigDecimal divide(BigDecimal a, BigDecimal b) {
		return a.divide(b, MATH_CONTEXT);
	}

	public static Duration multiply(Duration a, double b) {
		return Duration.ofMillis((long) Math.floor(a.toMillis() * b));
	}

	public static Duration divide(Duration a, double b) {
		return Duration.ofMillis((long) Math.floor(a.toMillis() / b));
	}

	public static BigDecimal round(BigDecimal val, int decimals) {
		return val.setScale(decimals, ROUNDING_MODE);
	}

	public static BigDecimal round(BigDecimal val) {
		return round(val, 0);
	}

	public static double round(double val, int decimals) {
		var p = Math.pow(10, decimals);
		return Math.round(val * p) / p;
	}
}
