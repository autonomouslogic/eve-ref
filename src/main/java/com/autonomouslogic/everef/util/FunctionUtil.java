package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.functions.Function;

public class FunctionUtil {
	public static <T> Function<T, T> concat(Function<T, T>... functions) {
		if (functions.length == 0) {
			throw new NullPointerException();
		}
		return (v) -> {
			for (Function<T, T> f : functions) {
				v = f.apply(v);
			}
			return v;
		};
	}
}
