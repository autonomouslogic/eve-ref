package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.cli.ImportIndustryResources.RIGS_CONFIG;

import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.api.SystemSecurity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.util.List;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class IndustryRigs extends AbstractIndustryService<IndustryRig> {
	@Inject
	protected IndustryRigs(CsvMapper csvMapper) {
		super(IndustryRig.class, RIGS_CONFIG, IndustryRig::getTypeId, csvMapper);
	}

	public double rigModifier(
			List<IndustryRig> rigs,
			InventoryType productType,
			SystemSecurity systemSecurity,
			Function<IndustryRig, Double> bonusGetter,
			String activity) {
		var bonus = 0.0;
		if (rigs != null) {
			for (var rig : rigs) {
				bonus += rigBonus(rig, productType, systemSecurity, bonusGetter, activity);
			}
		}
		return 1.0 + bonus;
	}

	public double rigBonus(
			IndustryRig rig,
			InventoryType productType,
			SystemSecurity systemSecurity,
			Function<IndustryRig, Double> bonusGetter,
			String activity) {
		var globalActivities = rig.getGlobalActivities();
		if (globalActivities != null && globalActivities.contains(activity)) {
			return bonusGetter.apply(rig) * getRigSecurityModifier(rig, systemSecurity);
		}
		if (!activity.equals("manufacturing") && !activity.equals("reaction")) {
			return 0.0;
		}

		List<Long> categories;
		List<Long> groups;
		switch (activity) {
			case "manufacturing":
				categories = rig.getManufacturingCategories();
				groups = rig.getManufacturingGroups();
				break;
			case "reaction":
				categories = rig.getReactionCategories();
				groups = rig.getReactionGroups();
				break;
			default:
				throw new IllegalStateException(activity);
		}
		var category = productType.getCategoryId();
		var group = productType.getGroupId();
		if ((categories != null && categories.contains(category)) || (groups != null && groups.contains(group))) {
			return bonusGetter.apply(rig) * getRigSecurityModifier(rig, systemSecurity);
		}

		return 0.0;
	}

	public double getRigSecurityModifier(IndustryRig rig, SystemSecurity systemSecurity) {
		if (rig == null) {
			return 1.0;
		}
		if (systemSecurity == null) {
			return 1.0;
		}
		return switch (systemSecurity) {
			case HIGH_SEC -> rig.getHighSecMod();
			case LOW_SEC -> rig.getLowSecMod();
			case NULL_SEC -> rig.getNullSecMod();
			default -> throw new RuntimeException("Unknown security: " + systemSecurity);
		};
	}
}
