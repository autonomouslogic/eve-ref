package com.autonomouslogic.everef.model.api.search;

import com.autonomouslogic.everef.service.search.SearchService;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Searcher {
	private final Pattern queryPattern;
	private final Long searchId;
	private final boolean textSearch;

	public Searcher(Pattern queryPattern) {
		this.queryPattern = queryPattern;
		this.searchId = parseIdFromPattern(queryPattern);
		textSearch = searchId == null || Long.toString(searchId).length() >= SearchService.MIN_SEARCH_LENGTH;
	}

	private Long parseIdFromPattern(Pattern pattern) {
		String patternString = pattern.pattern();
		try {
			return Long.parseLong(patternString);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public Stream<SearchEntry> apply(Stream<SearchEntry> stream) {
		return stream.flatMap(entry -> {
			if (searchId != null && entry.getId() == searchId) {
				return Stream.of(entry.toBuilder().relevance(0L).build());
			}

			if (!textSearch) {
				return Stream.empty();
			}

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
