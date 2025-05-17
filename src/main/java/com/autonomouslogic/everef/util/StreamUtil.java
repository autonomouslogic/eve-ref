package com.autonomouslogic.everef.util;

import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StreamUtil {
	public static <T> Stream<T> concat(Stream<T>... streams) {
		Stream<T> result = Stream.empty();
		for (Stream<T> stream : streams) {
			result = Stream.concat(result, stream);
		}
		return result;
	}
}
