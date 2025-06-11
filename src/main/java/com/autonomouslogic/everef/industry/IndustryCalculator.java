package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.IndustryConstants.MAX_ME;
import static com.autonomouslogic.everef.industry.IndustryConstants.MAX_TE;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.CopyingCost;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.InventionCost;
import com.autonomouslogic.everef.model.api.ManufacturingCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintMaterial;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.EsiMarketPriceService;
import com.autonomouslogic.everef.util.EveConstants;
import java.util.List;
import java.util.Map;
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
	protected EsiMarketPriceService esiMarketPriceService;

	@Inject
	protected Provider<ManufactureCalculator> manufactureCalculatorProvider;

	@Inject
	protected Provider<InventionCalculator> inventionCalculatorProvider;

	@Inject
	protected Provider<CopyingCalculator> copyingCalculatorProvider;

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
		Objects.requireNonNull(blueprint, "blueprint");

		cost = IndustryCost.builder();
		if (productType != null) {
			return calculateFromProduct();
		} else {
			return calculateFromBlueprint();
		}
	}

	private IndustryCost calculateFromProduct() {
		boolean isBlueprint = Optional.ofNullable(productType.getBlueprint()).orElse(false);
		if (!isBlueprint) {
			if (blueprint.getActivities().containsKey("manufacturing")) {
				var manufacturingCost = calculateManufacturing();
				var inventionCost = calculateInventionForManufacturing(manufacturingCost);
				if (inventionCost != null) {
					addInvention(inventionCost);
					var me = Optional.ofNullable(industryCostInput.getMe());
					var te = Optional.ofNullable(industryCostInput.getTe());
					if (me.isEmpty() || te.isEmpty()) {
						manufacturingCost = calculateManufacturing(
								me.orElse(inventionCost.getMe()), te.orElse(inventionCost.getTe()));
					}
					var copyingCost = calculateCopyingForInvention(inventionCost);
					addCopying(copyingCost);
				} else if (productType.getMetaGroupId() == null
						|| productType.getMetaGroupId() == EveConstants.TECH_1_META_GROUP_ID) {
					var copyingCost = calculateCopying();
					addCopying(copyingCost);
				}
				addManufacturing(manufacturingCost);
			} else if (blueprint.getActivities().containsKey("reaction")) {
				var reactionCost = calculateReaction();
				addReaction(reactionCost);
			}
		} else {
			var inventionCost = calculateInvention();
			addInvention(inventionCost);
			var copyingCost = calculateCopyingForInvention(inventionCost);
			addCopying(copyingCost);
		}
		return cost.build();
	}

	private IndustryCost calculateFromBlueprint() {
		var manufacturing = blueprint.getActivities().get("manufacturing");
		if (manufacturing != null) {
			for (Map.Entry<Long, BlueprintMaterial> entry :
					manufacturing.getProducts().entrySet()) {
				var productType = refData.getType(entry.getValue().getTypeId());
				var manufacturingCost = calculateManufacturing(
						productType,
						blueprint,
						industryCostInput.getRuns(),
						industryCostInput.getMe(),
						industryCostInput.getTe());
				addManufacturing(manufacturingCost);
			}
		}

		var reaction = blueprint.getActivities().get("reaction");
		if (reaction != null) {
			for (Map.Entry<Long, BlueprintMaterial> entry :
					reaction.getProducts().entrySet()) {
				var productType = refData.getType(entry.getValue().getTypeId());
				var manufacturingCost = calculateReaction(productType, blueprint, industryCostInput.getRuns());
				addReaction(manufacturingCost);
			}
		}

		var invention = blueprint.getActivities().get("invention");
		if (invention != null) {
			for (Map.Entry<Long, BlueprintMaterial> entry :
					invention.getProducts().entrySet()) {
				var productType = refData.getType(entry.getValue().getTypeId());
				var inventionCost = calculateInvention(productType, blueprint, industryCostInput.getRuns());
				addInvention(inventionCost);
			}
		}

		var copying = blueprint.getActivities().get("copying");
		if (copying != null) {
			var copyingCost = calculateCopying(blueprint, industryCostInput.getRuns());
			addCopying(copyingCost);
		}

		return cost.build();
	}

	private ManufacturingCost calculateProduction(
			String activity, InventoryType productType, Blueprint blueprint, int runs, Integer me, Integer te) {
		var isBlueprint = Optional.ofNullable(productType.getBlueprint()).orElse(false);
		if (isBlueprint) {
			throw new IllegalStateException("productType is a blueprint");
		}
		var manufacturingCost = manufactureCalculatorProvider
				.get()
				.setActivity(activity)
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

	private ManufacturingCost calculateManufacturing(
			InventoryType productType, Blueprint blueprint, int runs, Integer me, Integer te) {
		return calculateProduction("manufacturing", productType, blueprint, runs, me, te);
	}

	private ManufacturingCost calculateManufacturing() {
		return calculateProduction(
				"manufacturing",
				productType,
				blueprint,
				industryCostInput.getRuns(),
				industryCostInput.getMe(),
				industryCostInput.getTe());
	}

	private ManufacturingCost calculateManufacturing(int me, int te) {
		return calculateProduction("manufacturing", productType, blueprint, industryCostInput.getRuns(), me, te);
	}

	private ManufacturingCost calculateReaction() {
		return calculateReaction(productType, blueprint, industryCostInput.getRuns());
	}

	private ManufacturingCost calculateReaction(InventoryType productType, Blueprint blueprint, int runs) {
		return calculateProduction("reaction", productType, blueprint, runs, 0, 0);
	}

	private InventionCost calculateInvention() {
		return calculateInvention(productType, blueprint, industryCostInput.getRuns());
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

	private InventionCost calculateInventionForManufacturing(ManufacturingCost manufacturingCost) {
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
		return inventionCost;
	}

	private CopyingCost calculateCopying() {
		return calculateCopying(blueprint, industryCostInput.getRuns());
	}

	private CopyingCost calculateCopyingForInvention(InventionCost inventionCost) {
		var inventingBlueprint = refData.getBlueprint(inventionCost.getBlueprintId());
		return calculateCopying(inventingBlueprint, inventionCost.getRuns());
	}

	private CopyingCost calculateCopying(Blueprint blueprint, double runs) {
		int ceilRuns = (int) Math.ceil(runs);
		double factor = runs / ceilRuns;
		var cost = copyingCalculatorProvider
				.get()
				.setIndustryCostInput(industryCostInput)
				.setBlueprint(blueprint)
				.setRuns(ceilRuns)
				.setStructure(structure)
				.setRigs(rigs)
				.calc();
		if (factor != 1.0) {
			cost = cost.multiply(factor);
		}
		return cost;
	}

	public void addManufacturing(ManufacturingCost manufacturingCost) {
		cost.manufacturing(String.valueOf(manufacturingCost.getProductId()), manufacturingCost);
	}

	private void addReaction(ManufacturingCost reactionCost) {
		cost.reaction(String.valueOf(reactionCost.getProductId()), reactionCost);
	}

	public void addInvention(InventionCost inventionCost) {
		cost.invention(String.valueOf(inventionCost.getProductId()), inventionCost);
	}

	public void addCopying(CopyingCost copyingCost) {
		cost.copying(String.valueOf(copyingCost.getProductId()), copyingCost);
	}
}
