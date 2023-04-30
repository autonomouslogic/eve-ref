package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.functions.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
