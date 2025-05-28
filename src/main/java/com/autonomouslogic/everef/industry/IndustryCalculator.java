package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.IndustryConstants.MAX_ME;
import static com.autonomouslogic.everef.industry.IndustryConstants.MAX_TE;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.InventionCost;
import com.autonomouslogic.everef.model.api.ManufacturingCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class IndustryCalculator {
	@Inject
	protected MarketPriceService marketPriceService;

	@Inject
	protected Provider<ManufactureCalculator> manufactureCalculatorProvider;

	@Inject
	protected Provider<InventionCalculator> inventionCalculatorProvider;

	@Inject
	protected IndustryMath industryMath;

	@Setter
	@NonNull
	private LoadedRefData refData;

	@Setter
	@NonNull
	private IndustryCostInput industryCostInput;

	@Setter
	@NonNull
	private InventoryType productType;

	@Setter
	@NonNull
	private Blueprint blueprint;

	@Setter
	private IndustryDecryptor decryptor;

	@Setter
	private IndustryStructure structure;

	@Setter
	private List<IndustryRig> rigs;

	private IndustryCost.Builder cost;

	@Inject
	protected IndustryCalculator() {}

	public IndustryCost calc() {
		Objects.requireNonNull(industryCostInput, "industryCostInput");
		Objects.requireNonNull(productType, "productType");
		Objects.requireNonNull(blueprint, "blueprint");

		cost = IndustryCost.builder();
		boolean isBlueprint = Optional.ofNullable(productType.getBlueprint()).orElse(false);
		if (!isBlueprint) {
			var manufacturingCost = handleManufacturing();
			var inventionCost = handleInventionForManufacturing(manufacturingCost);
			handleManufacturingForInvention(inventionCost);
		} else {
			handleInvention();
		}
		return cost.build();
	}

	private ManufacturingCost handleManufacturing() {
		return handleManufacturing(
				productType,
				blueprint,
				industryCostInput.getRuns(),
				industryCostInput.getMe(),
				industryCostInput.getTe());
	}

	private InventionCost handleInvention() {
		return handleInvention(productType, blueprint);
	}

	private ManufacturingCost handleManufacturing(
			InventoryType productType, Blueprint blueprint, int runs, Integer me, Integer te) {
		var manufacturingCost = calculateManufacturing(productType, blueprint, runs, me, te);
		cost.manufacturing(String.valueOf(manufacturingCost.getProductId()), manufacturingCost);
		return manufacturingCost;
	}

	private InventionCost handleInvention(InventoryType productType, Blueprint blueprint) {
		var inventionCost = calculateInvention(productType, blueprint, industryCostInput.getRuns());
		cost.invention(String.valueOf(inventionCost.getProductId()), inventionCost);
		return inventionCost;
	}

	private InventionCost handleInventionForManufacturing(ManufacturingCost manufacturingCost) {
		var productBlueprintTypeId = manufacturingCost.getBlueprintId();
		var productBlueprintType = refData.getType(productBlueprintTypeId);
		var sourceBlueprint = Optional.ofNullable(productBlueprintType)
				.flatMap(v -> Optional.ofNullable(v.getProducedByBlueprints()))
				.stream()
				.flatMap(v -> v.values().stream())
				.filter(v -> v.getBlueprintActivity().equals("invention"))
				.findFirst()
				.map(v -> refData.getBlueprint(v.getBlueprintTypeId()));
		if (sourceBlueprint.isEmpty()) {
			return null;
		}
		var inventionCost = calculateInvention(
				productBlueprintType, sourceBlueprint.get(), (int) (manufacturingCost.getRuns() * 10));
		var factor = manufacturingCost.getRuns() / inventionCost.getExpectedRuns();
		inventionCost = inventionCost.multiply(factor);
		cost.invention(String.valueOf(inventionCost.getProductId()), inventionCost);
		return inventionCost;
	}

	private ManufacturingCost handleManufacturingForInvention(InventionCost inventionCost) {
		return handleManufacturing(
				productType,
				blueprint,
				(long) Math.round(inventionCost.getExpectedRuns()),
				inventionCost.getMe(),
				inventionCost.getTe());
	}

	private ManufacturingCost calculateManufacturing(
			InventoryType productType, Blueprint blueprint, int runs, Integer me, Integer te) {
		if (Optional.ofNullable(productType.getBlueprint()).orElse(false)) {
			throw new IllegalStateException("productType is a blueprint");
		}
		var manufacturingCost = manufactureCalculatorProvider
				.get()
				.setIndustryCostInput(industryCostInput)
				.setBlueprint(blueprint)
				.setProductType(productType)
				.setRuns(runs)
				.setMe(Optional.ofNullable(me).orElse(MAX_ME))
				.setTe(Optional.ofNullable(te).orElse(MAX_TE))
				.setStructure(structure)
				.setRigs(rigs)
				.calc();
		return manufacturingCost;
	}

	private InventionCost calculateInvention(InventoryType productType, Blueprint blueprint, int runs) {
		if (!Optional.ofNullable(productType.getBlueprint()).orElse(false)) {
			throw new IllegalStateException("productType is not a blueprint");
		}
		var inventionCost = inventionCalculatorProvider
				.get()
				.setIndustryCostInput(industryCostInput)
				.setProductType(productType)
				.setRuns(runs)
				.setBlueprint(blueprint)
				.setDecryptor(decryptor)
				.setStructure(structure)
				.setRigs(rigs)
				.calc();
		return inventionCost;
	}
}
