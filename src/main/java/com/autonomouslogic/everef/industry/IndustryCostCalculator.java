package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.api.ActivityCost;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;

public class IndustryCostCalculator {
	@Inject
	protected MarketPriceService marketPriceService;

	@Setter
	@NonNull
	private IndustryCostInput industryCostInput;

	@Setter
	@NonNull
	private InventoryType productType;

	@Setter
	@NonNull
	private Blueprint blueprint;

	@Inject
	protected IndustryCostCalculator() {}

	public IndustryCost calc() {
		Objects.requireNonNull(industryCostInput, "industryCostInput");
		Objects.requireNonNull(productType, "productType");
		Objects.requireNonNull(blueprint, "blueprint");

		var manufacturing = blueprint.getActivities().get("manufacturing");
		var manufacturingCost = manufacturingCost(manufacturing);
		return IndustryCost.builder()
				.manufacturing(String.valueOf(productType.getTypeId()), manufacturingCost)
				.build();
	}

	private ActivityCost manufacturingCost(BlueprintActivity manufacturing) {
		var time = manufacturingTime(manufacturing);
		var eiv = manufacturingEiv(manufacturing);
		var quantity = industryCostInput.getRuns();
		var systemCostIndex = manufacturingSystemCostIndex(eiv);
		var facilityTax = facilityTax(eiv);
		var sccSurcharge = sccSurcharge(eiv);
		var alphaCloneTax = alphaCloneTax(eiv);
		var totalJobCost = systemCostIndex.add(facilityTax).add(sccSurcharge).add(alphaCloneTax);
		return ActivityCost.builder()
				.productId(industryCostInput.getProductId())
				.quantity(quantity)
				.materials(manufacturingMaterials(manufacturing))
				.time(time)
				.timePerUnit(time.dividedBy(quantity).truncatedTo(ChronoUnit.MILLIS))
				.estimatedItemValue(eiv)
				.systemCostIndex(systemCostIndex)
				.facilityTax(facilityTax)
				.sccSurcharge(sccSurcharge)
				.alphaCloneTax(alphaCloneTax)
				.totalJobCost(totalJobCost)
				.build();
	}

	private Map<String, MaterialCost> manufacturingMaterials(BlueprintActivity manufacturing) {
		var materials = new LinkedHashMap<String, MaterialCost>();
		for (var material : manufacturing.getMaterials().values()) {
			materials.put(
					String.valueOf(material.getTypeId()),
					MaterialCost.builder()
							.typeId(material.getTypeId())
							.quantity(manufacturingMaterialQuantity(material.getQuantity()))
							.build());
		}
		return materials;
	}

	private long manufacturingMaterialQuantity(long base) {
		var runs = industryCostInput.getRuns();
		var meMod = 1.0 - industryCostInput.getMe() / 100.0;
		return (long) Math.max(runs, Math.ceil(Math.round(runs * base * meMod * 100.0) / 100.0));
	}

	private Duration manufacturingTime(BlueprintActivity manufacturing) {
		var baseTime = (double) manufacturing.getTime();
		var teMod = 1.0 - industryCostInput.getTe() / 100.0;
		var runs = industryCostInput.getRuns();
		var industryMod =
				1.0 - SkillIndustryBonuses.GLOBAL_TIME_BONUSES.get("Industry") * industryCostInput.getIndustry();
		var advancedIndustryMod = 1.0
				- SkillIndustryBonuses.GLOBAL_TIME_BONUSES.get("Advanced Industry")
						* industryCostInput.getAdvancedIndustry();
		return Duration.ofSeconds((long) Math.ceil(runs * baseTime * teMod * industryMod * advancedIndustryMod));
	}

	private BigDecimal manufacturingEiv(BlueprintActivity activityCost) {
		var eiv = BigDecimal.ZERO;
		for (var material : activityCost.getMaterials().values()) {
			var adjPrice = marketPriceService.getEsiAdjustedPrice(material.getTypeId());
			if (adjPrice.isEmpty()) {
				throw new RuntimeException("typeId: " + material.getTypeId());
			}
			eiv = eiv.add(
					BigDecimal.valueOf(material.getQuantity()).multiply(BigDecimal.valueOf(adjPrice.getAsDouble())));
		}
		return eiv.setScale(0, RoundingMode.HALF_UP);
	}

	private BigDecimal manufacturingSystemCostIndex(BigDecimal eiv) {
		var index = industryCostInput.getManufacturingCost();
		var cost = eiv.multiply(index).setScale(0, RoundingMode.HALF_UP);
		return cost;
	}

	private BigDecimal sccSurcharge(BigDecimal eiv) {
		return IndustryConstants.SCC_SURCHARGE_RATE.multiply(eiv).setScale(0, RoundingMode.HALF_UP);
	}

	private BigDecimal alphaCloneTax(BigDecimal eiv) {
		if (industryCostInput.getAlpha()) {
			return IndustryConstants.ALPHA_CLONE_TAX.multiply(eiv).setScale(0, RoundingMode.HALF_UP);
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal facilityTax(BigDecimal eiv) {
		return industryCostInput.getFacilityTax().multiply(eiv).setScale(0, RoundingMode.HALF_UP);
	}
}
