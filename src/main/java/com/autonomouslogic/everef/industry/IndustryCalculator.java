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
		} else {
			var manufacturingCost = manufactureCalculatorProvider
					.get()
					.setBlueprint(blueprint)
					.setProductType(productType)
					.setStructure(structure)
					.setRigs(rigs)
					.calc();
			builder.manufacturing(String.valueOf(productType.getTypeId()), manufacturingCost);
		}
		return builder.build();
	}
}
