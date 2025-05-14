package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.inject.DaggerMainComponent;
import com.autonomouslogic.everef.model.api.ActivityCost;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.BlueprintMaterial;
import com.autonomouslogic.everef.refdata.InventoryType;
import javax.inject.Inject;

import lombok.NonNull;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

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
		for (var material : manufacturing.getMaterials().values()) {
			cost.material(String.valueOf(material.getTypeId()), MaterialCost.builder()
					.typeId(material.getTypeId())
					.quantity(materialQuantity(material.getQuantity()))
				.build());
		}
		var eiv = getEstimatedItemValue(cost.build());
		return cost.build();
	}

	private long materialQuantity(long quantity) {
		var runs = industryCostInput.getRuns();
		var me = industryCostInput.getMe();
		var meMod = 1.0 - me / 100.0;
		return (long) Math.max(runs, Math.ceil(Math.round(runs * quantity * meMod * 100.0)/100.0));
	}

	private BigDecimal getEstimatedItemValue(ActivityCost activityCost) {
		var eiv = BigDecimal.ZERO;
		for (var material : activityCost.getMaterials().values()) {
			eiv = eiv.add(BigDecimal.valueOf(material.getQuantity())); // @todo
		}
		return eiv;
	}
}
