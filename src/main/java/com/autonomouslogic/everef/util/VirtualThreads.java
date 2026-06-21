package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

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
	 * Executes multiple tasks in parallel on the virtual thread pool with the default
	 * concurrency level (number of available processors).
	 * Uses commons-java's improved VirtualThreads.callAll implementation.
	 * @param tasks List of callable tasks to execute in parallel
	 * @return List of results in the same order as the input tasks
	 * @param <T> The return type of the tasks
	 */
	public static <T> List<T> parallel(List<? extends Callable<T>> tasks) {
		try {
			return com.autonomouslogic.commons.concurrent.VirtualThreads.callAll(
					tasks.iterator(), Runtime.getRuntime().availableProcessors());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while executing parallel tasks", e);
		} catch (java.util.concurrent.ExecutionException e) {
			throw new RuntimeException("Failed to execute parallel task", e);
		}
	}

	/**
	 * Executes multiple tasks in parallel on the virtual thread pool with controlled concurrency.
	 * Uses commons-java's improved VirtualThreads.callAll implementation.
	 * @param tasks List of callable tasks to execute in parallel
	 * @param concurrency Maximum number of tasks to execute concurrently
	 * @return List of results in the same order as the input tasks
	 * @param <T> The return type of the tasks
	 */
	public static <T> List<T> parallel(List<? extends Callable<T>> tasks, int concurrency) {
		try {
			return com.autonomouslogic.commons.concurrent.VirtualThreads.callAll(tasks.iterator(), concurrency);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while executing parallel tasks", e);
		} catch (java.util.concurrent.ExecutionException e) {
			throw new RuntimeException("Failed to execute parallel task", e);
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

	/**
	 * Executes multiple items in parallel using a provided executor function with default concurrency.
	 * This avoids constructing intermediate Callable objects and works directly with items.
	 * Uses commons-java's improved parallel execution.
	 * @param items Iterator of items to process
	 * @param executor Function to execute on each item
	 * @return List of results in the same order as the input items
	 * @param <T> The type of items
	 * @param <R> The return type of the executor function
	 */
	public static <T, R> List<R> parallel(Iterator<T> items, Function<T, R> executor) {
		return parallel(items, executor, Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Executes multiple items in parallel using a provided executor function with controlled concurrency.
	 * This avoids constructing intermediate Callable objects and works directly with items.
	 * Uses commons-java's improved parallel execution.
	 * @param items Iterator of items to process
	 * @param executor Function to execute on each item
	 * @param concurrency Maximum number of items to execute concurrently
	 * @return List of results in the same order as the input items
	 * @param <T> The type of items
	 * @param <R> The return type of the executor function
	 */
	public static <T, R> List<R> parallel(Iterator<T> items, Function<T, R> executor, int concurrency) {
		try {
			return com.autonomouslogic.commons.concurrent.VirtualThreads.callAll(
					items, (Function<T, R>) executor, concurrency);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while executing parallel tasks", e);
		} catch (java.util.concurrent.ExecutionException e) {
			throw new RuntimeException("Failed to execute parallel task", e);
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
