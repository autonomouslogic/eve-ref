package com.autonomouslogic.everef.ids;

/**
 * https://developers.eveonline.com/docs/guides/id-ranges/
 */
public class IdRanges {
	public static final IdRangeList REGION_IDS = new IdRangeList(
			new IdRange("eve", 10_000_000, 10_999_999),
			new IdRange("wormhole", 11_000_000, 11_999_999),
			new IdRange("abyssal", 12_000_000, 12_999_999),
			new IdRange("void", 14_000_000, 14_999_999),
			new IdRange("hidden", 19_000_000, 19_999_999));
}
