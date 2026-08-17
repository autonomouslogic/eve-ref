package com.autonomouslogic.everef.cli.publiccontracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

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
