package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.reactivex.rxjava3.functions.Supplier;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class VirtualThreadsTest {
	@Test
	@SneakyThrows
	void shouldExecuteParallelTasksConcurrently() {
		// Test that parallel tasks run concurrently (not sequentially)
		int taskCount = 4;
		long taskDuration = 500; // ms

		var timestamps = new ArrayList<Long>();
		var lock = new Object();

		var tasks = new ArrayList<Supplier<Void>>();
		for (int i = 0; i < taskCount; i++) {
			tasks.add(() -> {
				synchronized (lock) {
					timestamps.add(System.currentTimeMillis());
				}
				try {
					Thread.sleep(taskDuration);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				return null;
			});
		}

		long startTime = System.currentTimeMillis();
		VirtualThreads.parallel(tasks, taskCount);
		long totalTime = System.currentTimeMillis() - startTime;

		// If tasks run sequentially: total time ≈ taskCount * taskDuration
		// If tasks run in parallel: total time ≈ taskDuration
		long sequentialTime = taskCount * taskDuration;
		long maxConcurrentTime = taskDuration * 2; // Allow some overhead

		// With concurrency, total time should be significantly less than sequential
		assertTrue(
				totalTime < sequentialTime * 0.8,
				"Tasks appear to be running sequentially. Total time: " + totalTime + "ms, expected < "
						+ (sequentialTime * 0.8) + "ms");
	}

	@Test
	@SneakyThrows
	void shouldRespectConcurrencyLimit() {
		// Test that concurrency semaphore limits concurrent execution
		int taskCount = 8;
		int concurrency = 2;
		long taskDuration = 200; // ms

		var activeCount = new AtomicInteger(0);
		var maxConcurrentSeen = new AtomicInteger(0);
		var lock = new Object();

		var tasks = new ArrayList<Supplier<Void>>();
		for (int i = 0; i < taskCount; i++) {
			tasks.add(() -> {
				int current = activeCount.incrementAndGet();
				synchronized (lock) {
					if (current > maxConcurrentSeen.get()) {
						maxConcurrentSeen.set(current);
					}
				}

				try {
					Thread.sleep(taskDuration);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}

				activeCount.decrementAndGet();
				return null;
			});
		}

		long startTime = System.currentTimeMillis();
		VirtualThreads.parallel(tasks, concurrency);
		long totalTime = System.currentTimeMillis() - startTime;

		// Max concurrent should be at most the concurrency limit
		assertTrue(
				maxConcurrentSeen.get() <= concurrency,
				"Concurrency exceeded limit. Max seen: " + maxConcurrentSeen.get() + ", limit: " + concurrency);

		// Time should reflect concurrency limit: roughly (taskCount / concurrency) * taskDuration
		long expectedMinTime = (taskCount / concurrency) * taskDuration;
		assertTrue(
				totalTime >= expectedMinTime * 0.8,
				"Total time doesn't reflect concurrency limit. Total: " + totalTime + "ms, expected >= "
						+ (expectedMinTime * 0.8) + "ms");
	}

	@Test
	void shouldMaintainResultOrder() {
		// Test that results are returned in the same order as input
		var tasks = new ArrayList<Supplier<String>>();
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
		var tasks = new ArrayList<Supplier<String>>();
		var results = VirtualThreads.parallel(tasks);

		assertEquals(0, results.size());
	}

	@Test
	void shouldUseProcessorCountByDefault() {
		// Test that default parallel() uses processor count as concurrency
		int processorCount = Runtime.getRuntime().availableProcessors();
		var activeCount = new AtomicInteger(0);
		var maxConcurrentSeen = new AtomicInteger(0);
		var lock = new Object();

		var tasks = new ArrayList<Supplier<Void>>();
		// Create many tasks to properly test concurrency
		for (int i = 0; i < processorCount * 4; i++) {
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
				maxConcurrentSeen.get() <= processorCount + 1, // Allow 1 extra for timing variance
				"Default concurrency should match processor count. Max seen: " + maxConcurrentSeen.get()
						+ ", processors: " + processorCount);
	}
}
