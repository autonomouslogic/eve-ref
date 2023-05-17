package com.autonomouslogic.everef.util;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Returns the recent timestamp that's at a specific time of day.
 */
@Singleton
public class LastCutoff {
	public static final LocalTime ESI_REFRESH = LocalTime.parse("11:05");

	@Inject
	protected LastCutoff() {}

	public ZonedDateTime getCutoff(ZonedDateTime now, LocalTime refresh) {
		var cutoff = now.with(refresh);
		if (now.isBefore(cutoff)) {
			cutoff = cutoff.minusDays(1);
		}
		return cutoff;
	}

	public Instant getEsiRefresh(Instant now) {
		return getCutoff(now.atZone(ZoneOffset.UTC), ESI_REFRESH).toInstant();
	}

	public Instant getEsiRefresh() {
		return getEsiRefresh(Instant.now());
	}
}
