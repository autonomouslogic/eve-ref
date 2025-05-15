package com.autonomouslogic.everef.model.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FacilityType {
	STATION(false),
	RAITARU(true),
	AZBEL(true),
	SOTIYO(true),
	DRACCOUS_FORTIZAR(true),
	HORIZON_FORTIZAR(true),
	MOREAU_FORTIZAR(true),
	OTHER_STRUCTURE(true);

	private final boolean structure;
}
