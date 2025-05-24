package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
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

	@Inject
	protected IndustryCalculator() {}

	public IndustryCost calc() {
		Objects.requireNonNull(industryCostInput, "industryCostInput");
		Objects.requireNonNull(productType, "productType");
		Objects.requireNonNull(blueprint, "blueprint");

		var builder = IndustryCost.builder();
		if (Optional.ofNullable(productType.getBlueprint()).orElse(false)) {
			handleInvention(builder);
		} else {
			handleManufacturing(builder);
			handleInventionForManufacturing(builder);
		}
		return builder.build();
	}

	private void handleManufacturing(IndustryCost.Builder builder) {
		var manufacturingCost = manufactureCalculatorProvider
				.get()
				.setIndustryCostInput(industryCostInput)
				.setBlueprint(blueprint)
				.setProductType(productType)
				.setStructure(structure)
				.setRigs(rigs)
				.calc();
		builder.manufacturing(String.valueOf(productType.getTypeId()), manufacturingCost);
	}

	private void handleInvention(IndustryCost.Builder builder) {
		handleInvention(builder, productType, blueprint);
	}

	private void handleInvention(IndustryCost.Builder builder, InventoryType productType, Blueprint blueprint) {
		var inventionCost = inventionCalculatorProvider
				.get()
				.setIndustryCostInput(industryCostInput)
				.setProductType(productType)
				.setBlueprint(blueprint)
				.setDecryptor(decryptor)
				.setStructure(structure)
				.setRigs(rigs)
				.calc();
		builder.invention(String.valueOf(productType.getTypeId()), inventionCost);
	}

	private void handleInventionForManufacturing(IndustryCost.Builder builder) {
		var productBlueprintType = refData.getType(blueprint.getBlueprintTypeId());
		var sourceBlueprint = Optional.ofNullable(refData.getType(blueprint.getBlueprintTypeId()))
				.flatMap(v -> Optional.ofNullable(v.getProducedByBlueprints()))
				.stream()
				.flatMap(v -> v.values().stream())
				.filter(v -> v.getBlueprintActivity().equals("invention"))
				.findFirst()
				.map(v -> refData.getBlueprint(v.getBlueprintTypeId()));
		if (sourceBlueprint.isEmpty()) {
			return;
		}
		handleInvention(builder, productBlueprintType, sourceBlueprint.get());
	}
}
