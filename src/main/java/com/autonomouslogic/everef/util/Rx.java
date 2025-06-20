package com.autonomouslogic.everef.util;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableTransformer;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.CompletionStage;

/**
 * @deprecated Use {@link VirtualThreads}
 */
@Deprecated
public class Rx {
	@Deprecated
	public static <T> FlowableTransformer<T, T> offloadFlowable() {
		return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(VirtualThreads.SCHEDULER);
	}

	@Deprecated
	public static <T> MaybeTransformer<T, T> offloadMaybe() {
		return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(VirtualThreads.SCHEDULER);
	}

	@Deprecated
	public static <T> SingleTransformer<T, T> offloadSingle() {
		return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(VirtualThreads.SCHEDULER);
	}

	@Deprecated
	public static CompletableTransformer offloadCompletable() {
		return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(VirtualThreads.SCHEDULER);
	}

	@Deprecated
	public static <T> Single<T> toSingle(CompletionStage<T> future) {
		if (Thread.currentThread().isVirtual()) {
			return Single.fromFuture(future.toCompletableFuture());
		} else {
			return Rx3Util.toSingle(future);
		}
	}

	@Deprecated
	public static <T> Maybe<T> toMaybe(CompletionStage<T> future) {
		if (Thread.currentThread().isVirtual()) {
			return Maybe.fromFuture(future.toCompletableFuture());
		} else {
			return Rx3Util.toMaybe(future);
		}
	}

	@Deprecated
	public static Completable toCompletable(CompletionStage<Void> future) {
		if (Thread.currentThread().isVirtual()) {
			return Completable.fromFuture(future.toCompletableFuture());
		} else {
			return Rx3Util.toCompletable(future);
		}
	}
}
