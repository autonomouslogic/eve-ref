package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
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
		var thread = Thread.currentThread();
		if (!thread.isVirtual() && !thread.getName().equals("Test worker")) {
			throw new RuntimeException(
					"Not on a virtual thread: " + Thread.currentThread().getName());
		}
		return Maybe.defer(() -> Maybe.fromOptional(Optional.ofNullable(supplier.get())))
				.subscribeOn(Schedulers.io())
				.blockingGet();
	}
}
