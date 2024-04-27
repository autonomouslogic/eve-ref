package com.autonomouslogic.everef.cli.structures;

import com.autonomouslogic.everef.openapi.esi.infrastructure.ApiResponse;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Response;

@Singleton
public class StructureScrapeHelper {
	@Inject
	protected StructureScrapeHelper() {}

	public Optional<Instant> getLastModified(ApiResponse<?> response) {
		return Optional.ofNullable(response.getHeaders().get("last-modified")).stream()
				.flatMap(l -> l.stream())
				.findFirst()
				.map(t -> ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME)
						.toInstant());
	}

	public Optional<Instant> getLastModified(Response response) {
		return Optional.ofNullable(response.header("last-modified"))
				.map(t -> ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME)
						.toInstant());
	}
}
