package com.autonomouslogic.everef.cli.publiccontracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Data;

@Data
public class ContractsScrapeMeta {
	@JsonProperty
	private String datasource;

	@JsonProperty(value = "scrape_start")
	private Instant scrapeStart;

	@JsonProperty(value = "scrape_end")
	private Instant scrapeEnd;
}
