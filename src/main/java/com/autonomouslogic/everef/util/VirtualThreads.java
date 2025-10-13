package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VirtualThreads {
	public static final ExecutorService EXECUTOR = Executors.newThreadPerTaskExecutor(
			Thread.ofVirtual().name("virtual").factory());
	public static final Scheduler SCHEDULER = Schedulers.from(EXECUTOR);

	// Separate executor for offloading blocking I/O operations
	private static final ExecutorService IO_EXECUTOR = Executors.newCachedThreadPool();

	/**
	 * Offloads a task to a separate IO thread pool.
	 * @param supplier the task to execute
	 * @return the result of the task
	 * @param <T> the return type
	 */
	public static <T> T offload(Callable<T> supplier) {
		checkThread();
		try {
			Future<T> future = IO_EXECUTOR.submit(supplier);
			return future.get();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new RuntimeException("Error during offload execution", e);
		}
	}

	/**
	 * Offloads a task to a separate IO thread pool.
	 * @param action the action to execute
	 */
	public static void offload(Runnable action) {
		checkThread();
		try {
			Future<?> future = IO_EXECUTOR.submit(action);
			future.get();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new RuntimeException("Error during offload execution", e);
		}
	}

	public static void checkThread() {
		var thread = Thread.currentThread();
		if (!thread.isVirtual() && !thread.getName().equals("Test worker")) {
			throw new RuntimeException(
					"Not on a virtual thread: " + Thread.currentThread().getName());
		}
	}
}
