package com.autonomouslogic.everef.industry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkillIndustryBonuses {
	public static final Map<String, Double> GLOBAL_TIME_BONUSES;

	static {
		// @todo this can be loaded from Dogma
		var bonuses = new HashMap<String, Double>();
		// https://everef.net/types/3380
		bonuses.put("Industry", 0.04);
		// https://everef.net/types/3403
		bonuses.put("Research", 0.05);
		// https://everef.net/types/3402
		bonuses.put("Science", 0.05);
		// https://everef.net/types/3388
		bonuses.put("Advanced Industry", 0.03);
		// https://everef.net/types/3409
		bonuses.put("Metallurgy", 0.05);
		// https://everef.net/types/3395
		bonuses.put("Advanced Small Ship Construction", 0.01);
		// https://everef.net/types/3396
		bonuses.put("Advanced Industrial Ship Construction", 0.01);
		// https://everef.net/types/3397
		bonuses.put("Advanced Medium Ship Construction", 0.01);
		// https://everef.net/types/3398
		bonuses.put("Advanced Large Ship Construction", 0.01);
		// https://everef.net/types/1143
		bonuses.put("High Energy Physics", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Plasma Physics", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Nanite Engineering", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Hydromagnetic Physics", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Amarr Starship Engineering", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Minmatar Starship Engineering", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Graviton Physics", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Laser Physics", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Electromagnetic Physics", 0.01);
		// https://everef.net/types/1144
		bonuses.put("Rocket Science", 0.01);
		// https://everef.net/types/1145
		bonuses.put("Gallente Starship Engineering", 0.01);
		// https://everef.net/types/1145
		bonuses.put("Nuclear Physics", 0.01);
		// https://everef.net/types/1145
		bonuses.put("Mechanical Engineering", 0.01);
		// https://everef.net/types/1145
		bonuses.put("Electronic Engineering", 0.01);
		// https://everef.net/types/1145
		bonuses.put("Caldari Starship Engineering", 0.01);
		// https://everef.net/types/1145
		bonuses.put("Quantum Physics", 0.01);
		// https://everef.net/types/1152
		bonuses.put("Molecular Engineering", 0.01);
		// https://everef.net/types/5230
		bonuses.put("Triglavian Quantum Engineering", 0.01);
		// https://everef.net/types/7772
		bonuses.put("Advanced Capital Ship Construction", 0.01);
		// https://everef.net/types/8105
		bonuses.put("Upwell Starship Engineering", 0.01);
		// https://everef.net/types/8189
		bonuses.put("Mutagenic Stabilization", 0.0);
		GLOBAL_TIME_BONUSES = Collections.unmodifiableMap(bonuses);
	}
}
