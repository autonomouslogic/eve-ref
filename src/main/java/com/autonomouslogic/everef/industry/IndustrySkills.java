package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.cli.ImportIndustryResources.SKILLS_CONFIG;

import com.autonomouslogic.everef.model.IndustrySkill;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.util.List;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

public class IndustrySkills extends AbstractIndustryService<IndustrySkill> {
	@Value
	public static class SkillBonus {
		long skillId;
		double bonus;
	}

	@Inject
	protected IndustrySkills(CsvMapper csvMapper) {
		super(IndustrySkill.class, SKILLS_CONFIG, IndustrySkill::getTypeId, csvMapper);
	}

	public double manufacturingTimeBonusMod(IndustryCostInput input) {
		return sumBonusMod(input, IndustrySkill::getManufacturingTimeBonus);
	}

	public double reactionTimeBonusMod(IndustryCostInput input) {
		return sumBonusMod(input, IndustrySkill::getReactionTimeBonus);
	}

	public double advancedIndustrySkillIndustryJobTimeBonusMod(IndustryCostInput input) {
		return sumBonusMod(input, IndustrySkill::getAdvancedIndustrySkillIndustryJobTimeBonus);
	}

	public double blueprintmanufactureTimeBonusMod(IndustryCostInput input) {
		return sumBonusMod(input, IndustrySkill::getBlueprintmanufactureTimeBonus);
	}

	public double copySpeedBonus(IndustryCostInput input) {
		return sumBonusMod(input, IndustrySkill::getCopySpeedBonus);
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

	public List<Pair<IndustrySkill, Integer>> encryptionSkills(IndustryCostInput input, BlueprintActivity activity) {
		return stream()
				.filter(skill -> skill.isEncryptionMethods())
				.filter(skill -> activity.getRequiredSkills().containsKey(skill.getTypeId()))
				.map(skill -> Pair.of(skill, skillLevel(skill, input)))
				.toList();
	}

	public double manufacturingSpecialisedSkillMod(IndustryCostInput input, BlueprintActivity activity) {
		var skills = stream()
				.filter(skill -> skill.getManufactureTimePerLevel() != null)
				.filter(skill -> activity.getRequiredSkills() != null)
				.filter(skill -> activity.getRequiredSkills().containsKey(skill.getTypeId()))
				.map(skill -> Pair.of(skill, skillLevel(skill, input)))
				.toList();
		var mod = 1.0;
		for (var pair : skills) {
			var bonus = pair.getLeft().getManufactureTimePerLevel();
			var levelBonus = bonus * pair.getRight();
			var levelMod = 1.0 + levelBonus / 100.0;
			mod *= levelMod;
		}
		return mod;
	}

	private Integer skillLevel(IndustrySkill skill, IndustryCostInput input) {
		return switch (skill.getName()) {
			case "Industry" -> input.getIndustry();
			case "Reactions" -> input.getReactions();
			case "Advanced Industry" -> input.getAdvancedIndustry();
			case "Research" -> input.getResearch();
			case "Science" -> input.getScience();
			case "Metallurgy" -> input.getMetallurgy();
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
			case "Core Subsystem Technology" -> input.getCoreSubsystemTechnology();
			case "Defensive Subsystem Technology" -> input.getDefensiveSubsystemTechnology();
			case "Offensive Subsystem Technology" -> input.getOffensiveSubsystemTechnology();
			case "Propulsion Subsystem Technology" -> input.getPropulsionSubsystemTechnology();
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
