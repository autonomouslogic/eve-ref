package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.IndustrySkills.SPECIAL_TIME_BONUSES;

import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SkillMath {
	@Inject
	protected SkillMath() {}

	public double manufacturingSpecialisedSkillMod(
			BlueprintActivity manufacturing, IndustryCostInput industryCostInput) {
		return sumSkillMod(
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Small Ship Construction"),
						industryCostInput.getAdvancedSmallShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Industrial Ship Construction"),
						industryCostInput.getAdvancedIndustrialShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Medium Ship Construction"),
						industryCostInput.getAdvancedMediumShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Large Ship Construction"),
						industryCostInput.getAdvancedLargeShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("High Energy Physics"),
						industryCostInput.getHighEnergyPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Plasma Physics"),
						industryCostInput.getPlasmaPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Nanite Engineering"),
						industryCostInput.getNaniteEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Hydromagnetic Physics"),
						industryCostInput.getHydromagneticPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Amarr Starship Engineering"),
						industryCostInput.getAmarrStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Minmatar Starship Engineering"),
						industryCostInput.getMinmatarStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Graviton Physics"),
						industryCostInput.getGravitonPhysics()),
				specialSkillBonus(
						manufacturing, SPECIAL_TIME_BONUSES.get("Laser Physics"), industryCostInput.getLaserPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Electromagnetic Physics"),
						industryCostInput.getElectromagneticPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Rocket Science"),
						industryCostInput.getRocketScience()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Gallente Starship Engineering"),
						industryCostInput.getGallenteStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Nuclear Physics"),
						industryCostInput.getNuclearPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Mechanical Engineering"),
						industryCostInput.getMechanicalEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Electronic Engineering"),
						industryCostInput.getElectronicEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Caldari Starship Engineering"),
						industryCostInput.getCaldariStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Quantum Physics"),
						industryCostInput.getQuantumPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Molecular Engineering"),
						industryCostInput.getMolecularEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Triglavian Quantum Engineering"),
						industryCostInput.getTriglavianQuantumEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Capital Ship Construction"),
						industryCostInput.getAdvancedCapitalShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Upwell Starship Engineering"),
						industryCostInput.getUpwellStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Mutagenic Stabilization"),
						industryCostInput.getMutagenicStabilization()));
	}

	private double specialSkillBonus(BlueprintActivity manufacturing, IndustrySkills.SkillBonus bonus, int level) {
		if (manufacturing.getRequiredSkills().containsKey(bonus.getSkillId())) {
			return bonus.getBonus() * level;
		}
		return 0.0;
	}

	private double sumSkillMod(double... bonuses) {
		var mod = 1.0;
		for (double bonus : bonuses) {
			mod *= 1.0 - bonus;
		}
		return mod;
	}
}
