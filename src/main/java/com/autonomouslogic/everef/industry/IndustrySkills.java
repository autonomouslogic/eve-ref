package com.autonomouslogic.everef.industry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IndustrySkills {
	public static final Map<String, Double> GLOBAL_TIME_BONUSES;
	public static final Map<String, SkillBonus> SPECIAL_TIME_BONUSES;
	public static final Map<String, Long> ENCRYPTION_SKILLS;

	@Value
	public static class SkillBonus {
		long skillId;
		double bonus;
	}

	static {
		// @todo this can be loaded from Dogma
		var global = new HashMap<String, Double>();
		// https://everef.net/types/3380
		global.put("Industry", 0.04);
		// https://everef.net/types/3403
		global.put("Research", 0.05);
		// https://everef.net/types/3402
		global.put("Science", 0.05);
		// https://everef.net/types/3388
		global.put("Advanced Industry", 0.03);
		// https://everef.net/types/3409
		global.put("Metallurgy", 0.05);
		GLOBAL_TIME_BONUSES = Collections.unmodifiableMap(global);

		var special = new HashMap<String, SkillBonus>();
		// https://everef.net/types/3395
		special.put("Advanced Small Ship Construction", new SkillBonus(3395, 0.01));
		// https://everef.net/types/3396
		special.put("Advanced Industrial Ship Construction", new SkillBonus(3396, 0.01));
		// https://everef.net/types/3397
		special.put("Advanced Medium Ship Construction", new SkillBonus(3397, 0.01));
		// https://everef.net/types/3398
		special.put("Advanced Large Ship Construction", new SkillBonus(3398, 0.01));
		// https://everef.net/types/11433
		special.put("High Energy Physics", new SkillBonus(11433, 0.01));
		// https://everef.net/types/11441
		special.put("Plasma Physics", new SkillBonus(11441, 0.01));
		// https://everef.net/types/11442
		special.put("Nanite Engineering", new SkillBonus(11442, 0.01));
		// https://everef.net/types/11443
		special.put("Hydromagnetic Physics", new SkillBonus(11443, 0.01));
		// https://everef.net/types/11444
		special.put("Amarr Starship Engineering", new SkillBonus(11444, 0.01));
		// https://everef.net/types/11445
		special.put("Minmatar Starship Engineering", new SkillBonus(11445, 0.01));
		// https://everef.net/types/11446
		special.put("Graviton Physics", new SkillBonus(11446, 0.01));
		// https://everef.net/types/11447
		special.put("Laser Physics", new SkillBonus(11447, 0.01));
		// https://everef.net/types/11448
		special.put("Electromagnetic Physics", new SkillBonus(11448, 0.01));
		// https://everef.net/types/11449
		special.put("Rocket Science", new SkillBonus(11449, 0.01));
		// https://everef.net/types/11450
		special.put("Gallente Starship Engineering", new SkillBonus(11450, 0.01));
		// https://everef.net/types/11451
		special.put("Nuclear Physics", new SkillBonus(11451, 0.01));
		// https://everef.net/types/11452
		special.put("Mechanical Engineering", new SkillBonus(11452, 0.01));
		// https://everef.net/types/11453
		special.put("Electronic Engineering", new SkillBonus(11453, 0.01));
		// https://everef.net/types/11454
		special.put("Caldari Starship Engineering", new SkillBonus(11454, 0.01));
		// https://everef.net/types/11455
		special.put("Quantum Physics", new SkillBonus(11455, 0.01));
		// https://everef.net/types/11529
		special.put("Molecular Engineering", new SkillBonus(11529, 0.01));
		// https://everef.net/types/52307
		special.put("Triglavian Quantum Engineering", new SkillBonus(52307, 0.01));
		// https://everef.net/types/77725
		special.put("Advanced Capital Ship Construction", new SkillBonus(77725, 0.01));
		// https://everef.net/types/81050
		special.put("Upwell Starship Engineering", new SkillBonus(81050, 0.01));
		// https://everef.net/types/81896
		special.put("Mutagenic Stabilization", new SkillBonus(81896, 0.02));
		SPECIAL_TIME_BONUSES = Collections.unmodifiableMap(special);

		var encryption = new HashMap<String, Long>();
		encryption.put("Amarr Encryption Methods", 23087L);
		encryption.put("Caldari Encryption Methods", 21790L);
		encryption.put("Gallente Encryption Methods", 23121L);
		encryption.put("Minmatar Encryption Methods", 21791L);
		encryption.put("Sleeper Encryption Methods", 3408L);
		encryption.put("Triglavian Encryption Methods", 52308L);
		encryption.put("Upwell Encryption Methods", 55025L);
		ENCRYPTION_SKILLS = Collections.unmodifiableMap(encryption);
	}
}
