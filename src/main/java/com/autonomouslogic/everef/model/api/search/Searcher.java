package com.autonomouslogic.everef.model.api.search;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Searcher {
	private final Pattern queryPattern;

	public Stream<SearchEntry> apply(Stream<SearchEntry> stream) {
		return stream.flatMap(entry -> {
			var q = queryable(entry);
			if (q.isEmpty()) {
				return Stream.empty();
			}
			var matcher = queryPattern.matcher(q.get());
			if (!matcher.find()) {
				return Stream.empty();
			}
			return Stream.of(
					entry.toBuilder().relevance(relevance(matcher, q.get())).build());
		});
	}

	private Optional<String> queryable(SearchEntry entry) {
		return Optional.ofNullable(entry.getQuery()).or(() -> Optional.ofNullable(entry.getTitle()));
	}

	private long relevance(Matcher matcher, String queryable) {
		return (Math.min(255L, matcher.start()) << 8) + Math.min(256L, queryable.length());
	}
}
