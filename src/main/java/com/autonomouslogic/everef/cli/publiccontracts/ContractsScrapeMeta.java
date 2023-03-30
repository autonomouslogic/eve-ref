package com.autonomouslogic.everef.cli.publiccontracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class ContractsScrapeMeta {
	@JsonProperty
	private String server;
	@JsonProperty
	private String source;
	@JsonProperty(value = "scrape_start")
	private Instant scrapeStart;
	@JsonProperty(value = "scrape_end")
	private Instant scrapeEnd;
}
