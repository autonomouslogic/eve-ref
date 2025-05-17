package com.autonomouslogic.everef.util;

import java.math.MathContext;
import java.math.RoundingMode;

public class MathUtil {
	public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
	public static final MathContext MATH_CONTEXT = new MathContext(MathContext.DECIMAL128.getPrecision(), ROUNDING_MODE);
}
