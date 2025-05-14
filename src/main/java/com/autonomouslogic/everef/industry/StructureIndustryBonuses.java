package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.api.StructureClass;
import java.util.Map;

public class StructureIndustryBonuses {
	private static final Map<StructureClass, Double> MATERIAL_BONUSES = Map.of(
			StructureClass.STATION, 0.0,
			StructureClass.RAITARU, 0.01,
			StructureClass.AZBEL, 0.01,
			StructureClass.SOTIYO, 0.01,
			StructureClass.DRACCOUS_FORTIZAR, 0.0, // @todo
			StructureClass.HORIZON_FORTIZAR, 0.0, // @todo
			StructureClass.MOREAU_FORTIZAR, 0.0 // @todo
			);

	private static final Map<StructureClass, Double> TIME_BONUSES = Map.of(
			StructureClass.STATION, 0.0,
			StructureClass.RAITARU, 0.15,
			StructureClass.AZBEL, 0.20,
			StructureClass.SOTIYO, 0.30,
			StructureClass.DRACCOUS_FORTIZAR, 0.0, // @todo
			StructureClass.HORIZON_FORTIZAR, 0.0, // @todo
			StructureClass.MOREAU_FORTIZAR, 0.0 // @todo
			);

	private static final Map<StructureClass, Double> COST_BONUSES = Map.of(
			StructureClass.STATION, 0.0,
			StructureClass.RAITARU, 0.03,
			StructureClass.AZBEL, 0.04,
			StructureClass.SOTIYO, 0.05,
			StructureClass.DRACCOUS_FORTIZAR, 0.0, // @todo
			StructureClass.HORIZON_FORTIZAR, 0.0, // @todo
			StructureClass.MOREAU_FORTIZAR, 0.0 // @todo
			);
}
