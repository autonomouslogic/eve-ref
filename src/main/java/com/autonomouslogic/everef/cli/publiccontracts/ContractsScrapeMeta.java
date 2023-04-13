package com.autonomouslogic.everef.cli.publiccontracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ContractsScrapeMeta {
	@JsonProperty
	private String datasource;

	@JsonProperty
	private Instant scrapeStart;

	@JsonProperty
	private Instant scrapeEnd;
}
