package com.autonomouslogic.everef.util;

import java.util.List;

public class EveConstants {
	/**
	 * Universe IDs with known markets.
	 */
	public static final List<String> MARKET_UNIVERSE_IDS = List.of("eve", "wormhole");

	public static final int SHIP_CATEGORY_ID = 6;

	public static final int CITADELS_MARKET_GROUP_ID = 2199;
	public static final int ENGINEERING_COMPLEXES_MARKET_GROUP_ID = 2324;
	public static final int REFINERIES_MARKET_GROUP_ID = 2327;
	public static final int DECRYPTORS_MARKET_GROUP_ID = 1873;

	public static final long STANDARD_MARKET_HUB_I_TYPE_ID = 35892;
	public static final long ASTRAHUS_HUB_TYPE_ID = 35832;
	public static final long KEEPSTAR_TYPE_ID = 35834;

	public static final long NPC_STATION_MAX_ID = 100_000_000;
}
