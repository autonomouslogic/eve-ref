package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.config.Configs;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MemoryStatsLogger {
	private final ScheduledExecutorService scheduler;
	private final Duration interval;

	private MemoryStatsLogger(Duration interval) {
		this.interval = interval;
		this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
			var thread = new Thread(r, "MemoryStatsLogger");
			thread.setDaemon(true);
			return thread;
		});
	}

	private void start() {
		long delaySeconds = interval.getSeconds();
		scheduler.scheduleAtFixedRate(this::logMemoryStats, delaySeconds, delaySeconds, TimeUnit.SECONDS);
		log.debug("Memory statistics logger started with interval: {}", interval);
	}

	private void stop() {
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
		log.debug("Memory statistics logger stopped");
	}

	private void logMemoryStats() {
		try {
			MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
			MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

			long heapMax = heapUsage.getMax();
			long heapUsed = heapUsage.getUsed();
			long heapCommitted = heapUsage.getCommitted();
			double heapPercentage = (double) heapUsed / heapMax;

			MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
			long nonHeapUsed = nonHeapUsage.getUsed();
			long nonHeapCommitted = nonHeapUsage.getCommitted();

			// Garbage collection statistics
			List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
			String gcStats = gcBeans.stream()
					.map(gc -> String.format(
							"%s: collections=%d, time=%dms",
							gc.getName(), gc.getCollectionCount(), gc.getCollectionTime()))
					.collect(Collectors.joining(", "));

			log.debug(
					"Memory stats - Heap: {}/{} MiB ({}%), Committed: {} MiB - Non-Heap: {}/{} MB | GC: {}",
					formatMiB(heapUsed),
					formatMiB(heapMax),
					String.format("%.1f", heapPercentage * 100.0),
					formatMiB(heapCommitted),
					formatMiB(nonHeapUsed),
					formatMiB(nonHeapCommitted),
					gcStats);
		} catch (Exception e) {
			log.warn("Failed to log memory stats", e);
		}
	}

	private static String formatMiB(long bytes) {
		return String.format("%.0f", bytes / (1024.0 * 1024.0));
	}

	public static void startIfEnabled() {
		var interval = Configs.MEMORY_STATS_INTERVAL.get();
		if (interval.isPresent()) {
			MemoryStatsLogger logger = new MemoryStatsLogger(interval.get());
			logger.start();

			// Register shutdown hook to stop the logger
			Runtime.getRuntime().addShutdownHook(new Thread(logger::stop, "MemoryStatsLoggerShutdown"));
		}
	}
}
