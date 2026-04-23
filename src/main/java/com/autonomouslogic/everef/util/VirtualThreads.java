package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
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
	 * @param supplier The supplier task to execute
	 * @return The result of the task
	 * @param <T> The return type of the task
	 */
	public static <T> T run(Supplier<T> supplier) {
		checkThread();
		var future = EXECUTOR.submit(() -> {
			try {
				return supplier.get();
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
	 * Runs an action on the virtual thread pool.
	 * @param action The action to execute
	 */
	public static void run(Action action) {
		checkThread();
		var future = EXECUTOR.submit(() -> {
			try {
				action.run();
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
	 * Executes multiple tasks in parallel on the virtual thread pool with the default
	 * concurrency level (number of available processors).
	 * @param tasks List of supplier tasks to execute in parallel
	 * @return List of results in the same order as the input tasks
	 * @param <T> The return type of the tasks
	 */
	public static <T> List<T> parallel(List<? extends Supplier<T>> tasks) {
		return parallel(tasks, Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Executes multiple tasks in parallel on the virtual thread pool with controlled concurrency.
	 * @param tasks List of supplier tasks to execute in parallel
	 * @param concurrency Maximum number of tasks to execute concurrently
	 * @return List of results in the same order as the input tasks
	 * @param <T> The return type of the tasks
	 */
	public static <T> List<T> parallel(List<? extends Supplier<T>> tasks, int concurrency) {
		checkThread();
		if (tasks.isEmpty()) {
			return List.of();
		}

		var semaphore = new Semaphore(concurrency);
		var futures = new ArrayList<CompletableFuture<T>>(tasks.size());

		for (var task : tasks) {
			var future = CompletableFuture.supplyAsync(
					() -> {
						try {
							semaphore.acquire();
							try {
								return task.get();
							} finally {
								semaphore.release();
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new RuntimeException("Interrupted while executing parallel task", e);
						} catch (Throwable e) {
							throw new RuntimeException("Failed to execute parallel task", e);
						}
					},
					EXECUTOR);
			futures.add(future);
		}

		return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
	}

	/**
	 * Executes a task on a virtual thread. Can be called from any thread context.
	 * @param task The supplier task to execute
	 * @return The result of the task
	 * @param <T> The return type of the task
	 */
	@Deprecated(forRemoval = true)
	public static <T> T onVirtual(Supplier<T> task) {
		var future = EXECUTOR.submit(() -> {
			try {
				return task.get();
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
