package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.IndustryConstants.JOB_COST_BASE_RATE;

import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.model.api.PriceSource;
import com.autonomouslogic.everef.model.api.SecurityClass;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.FuzzworkMarketService;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.util.MathUtil;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;

@Singleton
public class IndustryMath {
	@Inject
	protected MarketPriceService marketPriceService;

	@Inject
	protected FuzzworkMarketService fuzzworkMarketService;

	@Inject
	protected IndustryStructures industryStructures;

	@Inject
	protected IndustryRigs industryRigs;

	@Inject
	protected IndustryMath() {}

	public BigDecimal eiv(BlueprintActivity activity, int runs) {
		var eiv = BigDecimal.ZERO;
		for (var material : activity.getMaterials().values()) {
			var adjPrice = marketPriceService.getEsiAdjustedPrice(material.getTypeId());
			if (adjPrice.isEmpty()) {
				throw new RuntimeException("typeId: " + material.getTypeId());
			}
			var quantity = BigDecimal.valueOf(material.getQuantity());
			var price = BigDecimal.valueOf(adjPrice.getAsDouble());
			var total = quantity.multiply(price);
			eiv = eiv.add(total);
		}
		return MathUtil.round(eiv.multiply(BigDecimal.valueOf(runs)));
	}

	public double efficiencyModifier(int score) {
		return 1.0 - score / 100.0;
	}

	public BigDecimal inventionSystemCostIndex(IndustryCostInput industryCostInput, BigDecimal jcb) {
		var index = industryCostInput.getInventionCost();
		return systemCostIndex(jcb, index);
	}

	public BigDecimal systemCostIndex(BigDecimal value, BigDecimal index) {
		var cost = MathUtil.round(value.multiply(index));
		return cost;
	}

	public Map<String, MaterialCost> materials(
			BlueprintActivity activity, Function<Long, Long> quantityMod, PriceSource priceSource) {
		var materials = new LinkedHashMap<String, MaterialCost>();
		for (var material : activity.getMaterials().values()) {
			long typeId = material.getTypeId();
			var quantity = quantityMod.apply(material.getQuantity());
			var costPerUnit = materialCostPerUnit(typeId, priceSource);
			var cost = materialCost(costPerUnit, quantity);
			materials.put(
					String.valueOf(typeId),
					MaterialCost.builder()
							.typeId(typeId)
							.quantity(quantity)
							.costPerUnit(costPerUnit)
							.cost(cost)
							.build());
		}
		return materials;
	}

	public BigDecimal materialCostPerUnit(long typeId, @NonNull PriceSource priceSource) {
		if (priceSource.isEsi()) {
			return MathUtil.round(
					BigDecimal.valueOf(
							marketPriceService.getEsiAveragePrice(typeId).orElse(0)),
					2);
		} else if (priceSource.isFuzzwork()) {
			var prices = fuzzworkMarketService.fetchAggregateStationPrices(priceSource.getStationId(), List.of(typeId));
			return Optional.ofNullable(prices.get(String.valueOf(typeId)))
					.flatMap(p -> switch (priceSource) {
						case FUZZWORK_JITA_BUY_AVG, FUZZWORK_JITA_BUY_MAX -> Optional.ofNullable(p.getBuy());
						case FUZZWORK_JITA_SELL_AVG, FUZZWORK_JITA_SELL_MIN -> Optional.ofNullable(p.getSell());
						default -> throw new RuntimeException(String.format("Unknown price source: %s", priceSource));
					})
					.flatMap(p -> switch (priceSource) {
						case FUZZWORK_JITA_BUY_AVG, FUZZWORK_JITA_SELL_AVG ->
							Optional.ofNullable(p.getWeightedAverage());
						case FUZZWORK_JITA_BUY_MAX -> Optional.ofNullable(p.getMax());
						case FUZZWORK_JITA_SELL_MIN -> Optional.ofNullable(p.getMin());
						default -> throw new RuntimeException(String.format("Unknown price source: %s", priceSource));
					})
					.orElse(BigDecimal.ZERO);
		}
		throw new RuntimeException(String.format("Unknown price source: %s", priceSource));
	}

	public BigDecimal materialCost(BigDecimal price, long quantity) {
		return MathUtil.round(price.multiply(BigDecimal.valueOf(quantity)), 2);
	}

	public BigDecimal jobCostBase(BigDecimal eiv) {
		return MathUtil.round(eiv.multiply(JOB_COST_BASE_RATE));
	}

	public BigDecimal systemCostBonuses(
			IndustryStructure structure,
			InventoryType productType,
			List<IndustryRig> rigs,
			SecurityClass securityClass,
			BigDecimal systemCostBonus,
			BigDecimal systemCostIndex,
			String activity) {
		var structureMod = industryStructures.structureCostModifier(structure);
		var rigMod = industryRigs.rigModifier(rigs, productType, securityClass, IndustryRig::getCostBonus, activity);
		var costMod = 1.0
				+ Optional.ofNullable(systemCostBonus).orElse(BigDecimal.ZERO).doubleValue();
		var mod = structureMod * rigMod * costMod;
		var modified = systemCostIndex.multiply(BigDecimal.valueOf(mod), MathUtil.MATH_CONTEXT);
		var bonus = modified.subtract(systemCostIndex);
		return MathUtil.round(bonus);
	}

	public BigDecimal sccSurcharge(BigDecimal val) {
		return MathUtil.round(IndustryConstants.SCC_SURCHARGE_RATE.multiply(val));
	}

	public BigDecimal alphaCloneTax(IndustryCostInput industryCostInput, BigDecimal val) {
		if (Optional.ofNullable(industryCostInput.getAlpha()).orElse(false)) {
			return MathUtil.round(IndustryConstants.ALPHA_CLONE_TAX.multiply(val));
		}
		return BigDecimal.ZERO;
	}

	public BigDecimal facilityTax(IndustryCostInput industryCostInput, BigDecimal val) {
		return MathUtil.round(industryCostInput.getFacilityTax().multiply(val));
	}

	public BigDecimal totalMaterialCost(Map<String, MaterialCost> materials) {
		var total = BigDecimal.ZERO;
		for (var materialCost : materials.values()) {
			total = total.add(materialCost.getCost());
		}
		return total;
	}
}
