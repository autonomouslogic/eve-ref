package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.cli.ImportIndustryResources.SKILLS_CONFIG;

import com.autonomouslogic.everef.model.IndustrySkill;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

public class IndustrySkills extends AbstractIndustryService<IndustrySkill> {
	@Deprecated
	public static final Map<String, SkillBonus> SPECIAL_TIME_BONUSES;

	@Deprecated
	public static final Map<String, Long> ENCRYPTION_SKILLS;

	@Value
	public static class SkillBonus {
		long skillId;
		double bonus;
	}

	static {
		// @todo this can be loaded from Dogma
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

	@Inject
	protected IndustrySkills(CsvMapper csvMapper) {
		super(IndustrySkill.class, SKILLS_CONFIG, IndustrySkill::getTypeId, csvMapper);
	}

	public double manufacturingTimeBonusMod(IndustryCostInput input) {
		return sumBonusMod(input, IndustrySkill::getManufacturingTimeBonus);
	}

	public double advancedIndustrySkillIndustryJobTimeBonusMod(IndustryCostInput input) {
		return sumBonusMod(input, IndustrySkill::getAdvancedIndustrySkillIndustryJobTimeBonus);
	}

	private double sumBonusMod(IndustryCostInput input, Function<IndustrySkill, Double> bonusGetter) {
		var skills = stream()
				.filter(skill -> bonusGetter.apply(skill) != null)
				.map(skill -> Pair.of(skill, skillLevel(skill, input)))
				.toList();
		var mod = 1.0;
		for (var pair : skills) {
			var bonus = bonusGetter.apply(pair.getLeft());
			var levelBonus = bonus * pair.getRight();
			var levelMod = 1.0 + levelBonus / 100.0;
			mod *= levelMod;
		}
		return mod;
	}

	public List<Pair<IndustrySkill, Integer>> datacoreSkills(IndustryCostInput input, BlueprintActivity activity) {
		return stream()
				.filter(skill -> skill.isDatacore())
				.filter(skill -> activity.getRequiredSkills().containsKey(skill.getTypeId()))
				.map(skill -> Pair.of(skill, skillLevel(skill, input)))
				.toList();
	}

	private Integer skillLevel(IndustrySkill skill, IndustryCostInput input) {
		return switch (skill.getName()) {
			case "Industry" -> input.getIndustry();
			case "Advanced Industry" -> input.getAdvancedIndustry();
			case "Advanced Small Ship Construction" -> input.getAdvancedSmallShipConstruction();
			case "Advanced Industrial Ship Construction" -> input.getAdvancedIndustrialShipConstruction();
			case "Advanced Medium Ship Construction" -> input.getAdvancedMediumShipConstruction();
			case "Advanced Large Ship Construction" -> input.getAdvancedLargeShipConstruction();
			case "High Energy Physics" -> input.getHighEnergyPhysics();
			case "Plasma Physics" -> input.getPlasmaPhysics();
			case "Nanite Engineering" -> input.getNaniteEngineering();
			case "Hydromagnetic Physics" -> input.getHydromagneticPhysics();
			case "Amarr Starship Engineering" -> input.getAmarrStarshipEngineering();
			case "Minmatar Starship Engineering" -> input.getMinmatarStarshipEngineering();
			case "Graviton Physics" -> input.getGravitonPhysics();
			case "Laser Physics" -> input.getLaserPhysics();
			case "Electromagnetic Physics" -> input.getElectromagneticPhysics();
			case "Rocket Science" -> input.getRocketScience();
			case "Gallente Starship Engineering" -> input.getGallenteStarshipEngineering();
			case "Nuclear Physics" -> input.getNuclearPhysics();
			case "Mechanical Engineering" -> input.getMechanicalEngineering();
			case "Electronic Engineering" -> input.getElectronicEngineering();
			case "Caldari Starship Engineering" -> input.getCaldariStarshipEngineering();
			case "Quantum Physics" -> input.getQuantumPhysics();
			case "Molecular Engineering" -> input.getMolecularEngineering();
			case "Triglavian Quantum Engineering" -> input.getTriglavianQuantumEngineering();
			case "Advanced Capital Ship Construction" -> input.getAdvancedCapitalShipConstruction();
			case "Upwell Starship Engineering" -> input.getUpwellStarshipEngineering();
			case "Mutagenic Stabilization" -> input.getMutagenicStabilization();
			case "Amarr Encryption Methods" -> input.getAmarrEncryptionMethods();
			case "Caldari Encryption Methods" -> input.getCaldariEncryptionMethods();
			case "Gallente Encryption Methods" -> input.getGallenteEncryptionMethods();
			case "Minmatar Encryption Methods" -> input.getMinmatarEncryptionMethods();
			case "Sleeper Encryption Methods" -> input.getSleeperEncryptionMethods();
			case "Triglavian Encryption Methods" -> input.getTriglavianEncryptionMethods();
			case "Upwell Encryption Methods" -> input.getUpwellEncryptionMethods();
			default -> throw new RuntimeException("Unhandled skill: " + skill.getName());
		};
	}
}
