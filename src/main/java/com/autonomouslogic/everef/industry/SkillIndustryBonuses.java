package com.autonomouslogic.everef.industry;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkillIndustryBonuses {
	public static final Map<String, Double> GLOBAL_TIME_BONUSES = Map.of(
			// https://everef.net/types/3380
			"Industry", 0.04,
			// https://everef.net/types/3403
			"Research", 0.05,
			// https://everef.net/types/3402
			"Science", 0.05,
			// https://everef.net/types/3388
			"Advanced Industry", 0.03,
			// https://everef.net/types/3409
			"Metallurgy", 0.05);
}
