package com.autonomouslogic.everef.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class RegionTypeMarketCap {
	int regionId;
	int typeId;
	BigDecimal cap;
}
