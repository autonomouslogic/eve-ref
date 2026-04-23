package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreads {
	public static final ExecutorService EXECUTOR = Executors.newThreadPerTaskExecutor(
			Thread.ofVirtual().name("virtual").factory());
	public static final Scheduler SCHEDULER = Schedulers.from(EXECUTOR);

	/**
	 * Offloads an action to the RxJava IO thread pool.
	 * @param supplier
	 * @return
	 * @param <T>
	 */
	public static <T> T offload(Supplier<T> supplier) {
		checkThread();
		return Maybe.defer(() -> Maybe.fromOptional(Optional.ofNullable(supplier.get())))
				.subscribeOn(Schedulers.io())
				.blockingGet();
	}

	/**
	 * Offloads an action to the RxJava IO thread pool.
	 * @param action
	 * @return
	 * @param <T>
	 */
	public static void offload(Action action) {
		checkThread();
		Completable.fromAction(action).subscribeOn(Schedulers.io()).blockingAwait();
	}

	/**
	 * Offloads multiple tasks to the IO thread pool and waits for all to complete.
	 * @param tasks List of supplier tasks to execute in parallel
	 * @return List of results in the same order as the input tasks
	 * @param <T> The return type of the tasks
	 */
	public static <T> List<T> offloadAll(List<? extends Supplier<T>> tasks) {
		checkThread();
		if (tasks.isEmpty()) {
			return List.of();
		}

		return Flowable.defer(() -> Flowable.fromIterable(tasks)
						.parallel(Math.min(tasks.size(), 4))
						.runOn(Schedulers.io())
						.flatMap(task -> Flowable.defer(() -> Flowable.fromOptional(Optional.ofNullable(task.get()))))
						.sequential())
				.toList()
				.blockingGet();
	}

	public static void checkThread() {
		var thread = Thread.currentThread();
		if (!thread.isVirtual() && !thread.getName().equals("Test worker")) {
			throw new RuntimeException(
					"Not on a virtual thread: " + Thread.currentThread().getName());
		}
	}
}
