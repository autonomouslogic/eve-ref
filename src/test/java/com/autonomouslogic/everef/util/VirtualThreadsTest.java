package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class VirtualThreadsTest {
	@Test
	@SneakyThrows
	void shouldExecuteParallelTasksConcurrently() {
		var count = 4;
		var duration = 500;

		var timestamps = new ArrayList<Long>();

		var tasks = new ArrayList<Callable<Void>>();
		for (int i = 0; i < count; i++) {
			tasks.add(() -> {
				synchronized (timestamps) {
					timestamps.add(System.currentTimeMillis());
				}
				try {
					Thread.sleep(duration);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				return null;
			});
		}

		long start = System.currentTimeMillis();
		VirtualThreads.parallel(tasks, count);
		long time = System.currentTimeMillis() - start;

		// If tasks run sequentially: total time = taskCount * taskDuration
		long sequentialTime = count * duration;

		// With concurrency, total time should be significantly less than sequential
		assertTrue(
				time < sequentialTime * 0.8,
				"Tasks appear to be running sequentially. Total time: " + time + "ms, expected < "
						+ (sequentialTime * 0.8) + "ms");
	}

	@Test
	@SneakyThrows
	void shouldRespectConcurrencyLimit() {
		int count = 8;
		int concurrency = 2;
		long duration = 200; // ms

		var activeCount = new AtomicInteger(0);
		var maxConcurrentSeen = new AtomicInteger(0);
		var lock = new Object();

		var tasks = new ArrayList<Callable<Void>>();
		for (int i = 0; i < count; i++) {
			tasks.add(() -> {
				int current = activeCount.incrementAndGet();
				synchronized (lock) {
					if (current > maxConcurrentSeen.get()) {
						maxConcurrentSeen.set(current);
					}
				}

				try {
					Thread.sleep(duration);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}

				activeCount.decrementAndGet();
				return null;
			});
		}

		long start = System.currentTimeMillis();
		VirtualThreads.parallel(tasks, concurrency);
		long time = System.currentTimeMillis() - start;

		assertTrue(
				maxConcurrentSeen.get() <= concurrency,
				"Concurrency exceeded limit. Max seen: " + maxConcurrentSeen.get() + ", limit: " + concurrency);

		long expectedMinTime = (count / concurrency) * duration;
		assertTrue(
				time >= expectedMinTime * 0.8,
				"Total time doesn't reflect concurrency limit. Total: " + time + "ms, expected >= "
						+ (expectedMinTime * 0.8) + "ms");
	}

	@Test
	void shouldMaintainResultOrder() {
		// Test that results are returned in the same order as input
		var tasks = new ArrayList<Callable<String>>();
		tasks.add(() -> "first");
		tasks.add(() -> "second");
		tasks.add(() -> "third");
		tasks.add(() -> "fourth");

		var results = VirtualThreads.parallel(tasks);

		assertEquals(4, results.size());
		assertEquals("first", results.get(0));
		assertEquals("second", results.get(1));
		assertEquals("third", results.get(2));
		assertEquals("fourth", results.get(3));
	}

	@Test
	void shouldHandleEmptyTaskList() {
		var tasks = new ArrayList<Callable<String>>();
		var results = VirtualThreads.parallel(tasks);

		assertEquals(0, results.size());
	}

	@Test
	void shouldUseProcessorCountByDefault() {
		// Test that default parallel() uses processor count as concurrency
		int processors = Runtime.getRuntime().availableProcessors();
		var activeCount = new AtomicInteger(0);
		var maxConcurrentSeen = new AtomicInteger(0);
		var lock = new Object();

		var tasks = new ArrayList<Callable<Void>>();
		// Create many tasks to properly test concurrency
		for (int i = 0; i < processors * 4; i++) {
			tasks.add(() -> {
				int current = activeCount.incrementAndGet();
				synchronized (lock) {
					if (current > maxConcurrentSeen.get()) {
						maxConcurrentSeen.set(current);
					}
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}

				activeCount.decrementAndGet();
				return null;
			});
		}

		VirtualThreads.parallel(tasks);

		// Should use processor count as concurrency limit
		assertTrue(
				maxConcurrentSeen.get() <= processors + 1, // Allow 1 extra for timing variance
				"Default concurrency should match processor count. Max seen: " + maxConcurrentSeen.get()
						+ ", processors: " + processors);
	}
}
