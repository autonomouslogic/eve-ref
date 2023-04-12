package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.CompletableTransformer;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.SingleTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Rx {
	public static <T> FlowableTransformer<T, T> offloadFlowable() {
		return offloadFlowable(Schedulers.io());
	}

	public static <T> FlowableTransformer<T, T> offloadFlowable(Scheduler scheduler) {
		return upstream -> upstream.subscribeOn(scheduler).observeOn(Schedulers.computation());
	}

	public static <T> MaybeTransformer<T, T> offloadMaybe() {
		return offloadMaybe(Schedulers.io());
	}

	public static <T> MaybeTransformer<T, T> offloadMaybe(Scheduler scheduler) {
		return upstream -> upstream.subscribeOn(scheduler).observeOn(Schedulers.computation());
	}

	public static <T> SingleTransformer<T, T> offloadSingle() {
		return offloadSingle(Schedulers.io());
	}

	public static <T> SingleTransformer<T, T> offloadSingle(Scheduler scheduler) {
		return upstream -> upstream.subscribeOn(scheduler).observeOn(Schedulers.computation());
	}

	public static CompletableTransformer offloadCompletable() {
		return offloadCompletable(Schedulers.io());
	}

	public static CompletableTransformer offloadCompletable(Scheduler scheduler) {
		return upstream -> upstream.subscribeOn(scheduler).observeOn(Schedulers.computation());
	}
}
