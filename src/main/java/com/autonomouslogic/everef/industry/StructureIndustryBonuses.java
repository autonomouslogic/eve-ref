package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.api.FacilityType;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StructureIndustryBonuses {
	public static final Map<FacilityType, Double> MATERIAL_BONUSES = Map.of(
			FacilityType.RAITARU, 0.01,
			FacilityType.AZBEL, 0.01,
			FacilityType.SOTIYO, 0.01,
			FacilityType.DRACCOUS_FORTIZAR, 0.0,
			FacilityType.HORIZON_FORTIZAR, 0.0,
			FacilityType.MOREAU_FORTIZAR, 0.0);

	public static final Map<FacilityType, Double> TIME_BONUSES = Map.of(
			FacilityType.RAITARU, 0.15,
			FacilityType.AZBEL, 0.20,
			FacilityType.SOTIYO, 0.30,
			FacilityType.DRACCOUS_FORTIZAR, 0.15,
			FacilityType.HORIZON_FORTIZAR, 0.15,
			FacilityType.MOREAU_FORTIZAR, 0.10);

	public static final Map<FacilityType, Double> COST_BONUSES = Map.of(
			FacilityType.RAITARU, 0.03,
			FacilityType.AZBEL, 0.04,
			FacilityType.SOTIYO, 0.05,
			FacilityType.DRACCOUS_FORTIZAR, 0.03,
			FacilityType.HORIZON_FORTIZAR, 0.03,
			FacilityType.MOREAU_FORTIZAR, 0.02);
}
