package com.autonomouslogic.everef.model.api;

import static com.autonomouslogic.everef.util.EveConstants.JITA_4_4_STATION_ID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PriceSource {
	ESI_AVG(true, false),
	FUZZWORK_JITA_SELL_MIN(false, true, JITA_4_4_STATION_ID),
	FUZZWORK_JITA_SELL_AVG(false, true, JITA_4_4_STATION_ID),
	FUZZWORK_JITA_BUY_MAX(false, true, JITA_4_4_STATION_ID),
	FUZZWORK_JITA_BUY_AVG(false, true, JITA_4_4_STATION_ID);

	PriceSource(boolean esi, boolean fuzzwork) {
		this(esi, fuzzwork, -1);
	}

	boolean esi;

	boolean fuzzwork;

	long stationId;
}
