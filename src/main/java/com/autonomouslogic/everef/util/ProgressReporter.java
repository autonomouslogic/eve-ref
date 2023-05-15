package com.autonomouslogic.everef.util;

import com.google.common.util.concurrent.AtomicDouble;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class ProgressReporter {
	@NonNull
	@Getter
	private final String name;

	@Getter
	private final double totalWork;

	@NonNull
	private final Duration reportInterval;

	@Getter
	private Instant startTime;

	@Getter
	private volatile Instant lastReportTime = Instant.MIN;

	private final AtomicDouble completedWork = new AtomicDouble();

	public void start() {
		startTime = Instant.now();
	}

	public void increment() {
		increment(1.0);
	}

	public void increment(double work) {
		Objects.requireNonNull(startTime, "start() must be called before increment()");
		completedWork.addAndGet(work);
		report();
	}

	private synchronized void report() {
		var elapsed = Duration.between(lastReportTime, Instant.now());
		if (elapsed.compareTo(reportInterval) < 0) {
			return;
		}
		lastReportTime = Instant.now();
		var runtime = getRuntime();
		var completed = getCompletedWork();
		var percent = getCompletedRatio() * 100.0;
		var rate = getRate();
		var remainingTime = getRemainingTime();
		log.info(String.format(
				"%s: %.1f completed (%.1f%%) in %s @ %.1f/s - est. %s remaining",
				name,
				completed,
				percent,
				runtime.truncatedTo(ChronoUnit.SECONDS),
				rate,
				remainingTime.truncatedTo(ChronoUnit.SECONDS)));
	}

	public Duration getRuntime() {
		return Duration.between(startTime, Instant.now());
	}

	public double getCompletedRatio() {
		return completedWork.get() / totalWork;
	}

	public double getCompletedWork() {
		return completedWork.get();
	}

	public double getRate() {
		return getCompletedWork() / getRuntime().toMillis() * 1000.0;
	}

	public Duration getRemainingTime() {
		return Duration.ofMillis((long) ((totalWork - getCompletedWork()) / getRate() * 1000.0));
	}
}
