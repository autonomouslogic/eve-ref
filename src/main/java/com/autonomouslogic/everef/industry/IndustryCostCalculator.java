package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.api.ActivityCost;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;

public class IndustryCostCalculator {
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
		var manufacturingCost = calcManufacturing(manufacturing);
		return IndustryCost.builder()
				.manufacturing(String.valueOf(productType.getTypeId()), manufacturingCost)
				.build();
	}

	private ActivityCost calcManufacturing(BlueprintActivity manufacturing) {
		var cost = ActivityCost.builder();
		calcManufacturingMaterials(manufacturing, cost);
		var time = calcManufacturingTime(manufacturing);
		cost.time(time);
		cost.timePerRun(time.dividedBy(industryCostInput.getRuns()));

		var eiv = getEstimatedItemValue(cost.build());
		return cost.build();
	}

	private void calcManufacturingMaterials(BlueprintActivity manufacturing, ActivityCost.Builder cost) {
		for (var material : manufacturing.getMaterials().values()) {
			cost.material(
					String.valueOf(material.getTypeId()),
					MaterialCost.builder()
							.typeId(material.getTypeId())
							.quantity(manufacturingMaterialQuantity(material.getQuantity()))
							.build());
		}
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

	private BigDecimal getEstimatedItemValue(ActivityCost activityCost) {
		var eiv = BigDecimal.ZERO;
		for (var material : activityCost.getMaterials().values()) {
			eiv = eiv.add(BigDecimal.valueOf(material.getQuantity())); // @todo
		}
		return eiv;
	}
}
