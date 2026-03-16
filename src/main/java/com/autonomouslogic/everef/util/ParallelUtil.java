package com.autonomouslogic.everef.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utilities for parallel execution using virtual threads.
 * This replaces RxJava's parallel execution patterns.
 */
public class ParallelUtil {

	/**
	 * Processes a collection of items in parallel with a specified level of concurrency.
	 *
	 * @param items the items to process
	 * @param concurrency the maximum number of concurrent executions
	 * @param processor the function to process each item
	 * @param <T> the input type
	 * @param <R> the result type
	 * @return a list of results in the same order as the input
	 */
	public static <T, R> List<R> processInParallel(Collection<T> items, int concurrency, Function<T, R> processor) {
		return processInParallel(items, concurrency, processor, VirtualThreads.EXECUTOR);
	}

	/**
	 * Processes a collection of items in parallel with a specified level of concurrency using a custom executor.
	 *
	 * @param items the items to process
	 * @param concurrency the maximum number of concurrent executions
	 * @param processor the function to process each item
	 * @param executor the executor service to use
	 * @param <T> the input type
	 * @param <R> the result type
	 * @return a list of results in the same order as the input
	 */
	public static <T, R> List<R> processInParallel(
			Collection<T> items, int concurrency, Function<T, R> processor, ExecutorService executor) {
		if (items.isEmpty()) {
			return List.of();
		}

		Semaphore semaphore = new Semaphore(concurrency);
		List<CompletableFuture<R>> futures = new ArrayList<>();

		for (T item : items) {
			CompletableFuture<R> future = CompletableFuture.supplyAsync(
					() -> {
						try {
							semaphore.acquire();
							try {
								return processor.apply(item);
							} finally {
								semaphore.release();
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new RuntimeException("Interrupted while waiting for semaphore", e);
						}
					},
					executor);
			futures.add(future);
		}

		// Wait for all futures to complete and collect results
		return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
	}

	/**
	 * Processes a collection of items in parallel with unlimited concurrency.
	 *
	 * @param items the items to process
	 * @param processor the function to process each item
	 * @param <T> the input type
	 * @param <R> the result type
	 * @return a list of results in the same order as the input
	 */
	public static <T, R> List<R> processInParallel(Collection<T> items, Function<T, R> processor) {
		return processInParallel(items, Integer.MAX_VALUE, processor);
	}

	/**
	 * Processes a collection of items in parallel, ignoring results (for side effects).
	 *
	 * @param items the items to process
	 * @param concurrency the maximum number of concurrent executions
	 * @param processor the function to process each item
	 * @param <T> the input type
	 */
	public static <T> void processInParallelVoid(Collection<T> items, int concurrency, Function<T, Void> processor) {
		processInParallel(items, concurrency, processor);
	}
}
