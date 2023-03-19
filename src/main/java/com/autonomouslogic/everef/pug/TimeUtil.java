package com.autonomouslogic.everef.pug;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;

/**
 *
 */
public class TimeUtil {
	private static final DateTimeFormatter isoLike = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

	private static final long DAY = Duration.ofDays(1).toMillis();
	private static final long HOUR = Duration.ofHours(1).toMillis();
	private static final long MINUTE = Duration.ofMinutes(1).toMillis();
	private static final long SECOND = Duration.ofSeconds(1).toMillis();

	@Inject
	public TimeUtil() {}

	public String until(Instant time) {
		long millis = Duration.between(Instant.now(), time).toMillis();
		int days = 0;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;
		if (millis > DAY) {
			days = (int) Math.floor((double) millis / (double) DAY);
			millis -= DAY * days;
		}
		if (millis > HOUR) {
			hours = (int) Math.floor((double) millis / (double) HOUR);
			millis -= HOUR * hours;
		}
		if (millis > MINUTE) {
			minutes = (int) Math.floor((double) millis / (double) MINUTE);
			millis -= MINUTE * minutes;
		}
		seconds = (int) Math.floor((double) millis / (double) SECOND);
		StringBuilder sb = new StringBuilder();
		if (days > 0) {
			sb.append(days).append("d ");
		}
		if (days > 0 || hours > 0) {
			sb.append(hours).append("h ");
		}
		if (days > 0 || hours > 0 || minutes > 0) {
			sb.append(minutes).append("m ");
		}
		sb.append(seconds).append("s");
		return sb.toString();
	}

	public String isoLike(Instant instant) {
		return isoLike(instant.atZone(ZoneId.of("UTC")));
	}

	public String isoLike(ZonedDateTime dateTime) {
		return isoLike.format(dateTime);
	}
}
