package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.api.FacilityType;
import java.util.Map;

public class StructureIndustryBonuses {
	private static final Map<FacilityType, Double> MATERIAL_BONUSES = Map.of(
			FacilityType.STATION, 0.0,
			FacilityType.RAITARU, 0.01,
			FacilityType.AZBEL, 0.01,
			FacilityType.SOTIYO, 0.01,
			FacilityType.DRACCOUS_FORTIZAR, 0.0,
			FacilityType.HORIZON_FORTIZAR, 0.0,
			FacilityType.MOREAU_FORTIZAR, 0.0);

	private static final Map<FacilityType, Double> TIME_BONUSES = Map.of(
			FacilityType.STATION, 0.0,
			FacilityType.RAITARU, 0.15,
			FacilityType.AZBEL, 0.20,
			FacilityType.SOTIYO, 0.30,
			FacilityType.DRACCOUS_FORTIZAR, 0.15,
			FacilityType.HORIZON_FORTIZAR, 0.15,
			FacilityType.MOREAU_FORTIZAR, 0.10);

	private static final Map<FacilityType, Double> COST_BONUSES = Map.of(
			FacilityType.STATION, 0.0,
			FacilityType.RAITARU, 0.03,
			FacilityType.AZBEL, 0.04,
			FacilityType.SOTIYO, 0.05,
			FacilityType.DRACCOUS_FORTIZAR, 0.03,
			FacilityType.HORIZON_FORTIZAR, 0.03,
			FacilityType.MOREAU_FORTIZAR, 0.02);
}
