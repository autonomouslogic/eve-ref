package com.autonomouslogic.everef.model.api.deeparchive;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Seeder counts for the Deep Archive torrents.")
public class DeepArchiveSeeders {
	@JsonProperty
	@Schema(description = "Number of seeders reported by the tracker, keyed by lowercase hex v1 info-hash.")
	Map<String, Integer> seeders;

	@JsonProperty
	@Schema(description = "When the tracker was scraped.")
	Instant scrapedAt;
}
