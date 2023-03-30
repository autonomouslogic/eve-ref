package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.CompletableTransformer;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.SingleTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Rx {
	public static <T> FlowableTransformer<T, T> offloadFlowable() {
		return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation());
	}

	public static <T> MaybeTransformer<T, T> offloadMaybe() {
		return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation());
	}

	public static <T> SingleTransformer<T, T> offloadSingle() {
		return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation());
	}

	public static CompletableTransformer offloadCompletable() {
		return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation());
	}
}
