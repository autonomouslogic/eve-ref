package com.autonomouslogic.everef.model.api.search;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Searcher {
	private final Pattern queryPattern;

	public Stream<SearchEntry> apply(Stream<SearchEntry> stream) {
		return stream.filter(entry -> queryPattern.matcher(entry.getTitle()).find());
	}
}
