package com.autonomouslogic.everef.industry;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IndustryConstants {
	public static final BigDecimal SCC_SURCHARGE_RATE = BigDecimal.valueOf(0.04);
	public static final BigDecimal ALPHA_CLONE_TAX = BigDecimal.valueOf(0.0025);
	public static final BigDecimal JOB_COST_BASE_RATE = BigDecimal.valueOf(0.02);
}
