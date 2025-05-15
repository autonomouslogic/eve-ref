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
		var cost = ActivityCost.builder();
		cost.materials(manufacturingMaterials(manufacturing));
		var time = calcManufacturingTime(manufacturing);
		cost.time(time);
		cost.timePerUnit(time.dividedBy(industryCostInput.getRuns()));
		cost.estimatedItemValue(estimatedItemValue(manufacturing));
		return cost.build();
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

	private Duration calcManufacturingTime(BlueprintActivity manufacturing) {
		var baseTime = (double) manufacturing.getTime();
		var teMod = 1.0 - industryCostInput.getTe() / 100.0;
		var runs = industryCostInput.getRuns();
		return Duration.ofSeconds((long) Math.ceil(runs * baseTime * teMod));
	}

	private BigDecimal estimatedItemValue(BlueprintActivity activityCost) {
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
}
