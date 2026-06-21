package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class VirtualThreads {
	public static final ExecutorService EXECUTOR = Executors.newThreadPerTaskExecutor(
			Thread.ofVirtual().name("virtual").factory());
	public static final Scheduler SCHEDULER = Schedulers.from(EXECUTOR);

	/**
	 * Runs a task on the virtual thread pool.
	 * @param callable The callable task to execute
	 * @return The result of the task
	 * @param <T> The return type of the task
	 */
	public static <T> T run(Callable<T> callable) {
		checkThread();
		var future = EXECUTOR.submit(() -> {
			try {
				return callable.call();
			} catch (Throwable e) {
				throw new RuntimeException("Task execution failed", e);
			}
		});

		try {
			return future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while executing task on virtual thread", e);
		} catch (java.util.concurrent.ExecutionException e) {
			throw new RuntimeException(
					"Failed to execute task on virtual thread", e.getCause() != null ? e.getCause() : e);
		}
	}

	/**
	 * Runs a runnable on the virtual thread pool.
	 * @param runnable The runnable to execute
	 */
	public static void run(Runnable runnable) {
		checkThread();
		var future = EXECUTOR.submit(() -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				throw new RuntimeException("Task execution failed", e);
			}
		});

		try {
			future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while executing task on virtual thread", e);
		} catch (java.util.concurrent.ExecutionException e) {
			throw new RuntimeException(
					"Failed to execute task on virtual thread", e.getCause() != null ? e.getCause() : e);
		}
	}

	/**
	 * Executes a task on a virtual thread. Can be called from any thread context.
	 * @param task The callable task to execute
	 * @return The result of the task
	 * @param <T> The return type of the task
	 */
	@Deprecated(forRemoval = true)
	public static <T> T onVirtual(Callable<T> task) {
		var future = EXECUTOR.submit(() -> {
			try {
				return task.call();
			} catch (Throwable e) {
				throw new RuntimeException("Task execution failed", e);
			}
		});

		try {
			return future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while executing task on virtual thread", e);
		} catch (java.util.concurrent.ExecutionException e) {
			throw new RuntimeException(
					"Failed to execute task on virtual thread", e.getCause() != null ? e.getCause() : e);
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
