package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.model.api.search.SearchEntry;
import com.autonomouslogic.everef.model.api.search.SearchResult;
import com.autonomouslogic.everef.model.api.search.Searcher;
import com.google.common.collect.Ordering;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class SearchService {
	public static int MIN_SEARCH_LENGTH = 3;
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

	private static final Comparator<SearchEntry> RELEVANCE_COMPARATOR =
			Ordering.natural().onResultOf(SearchEntry::getRelevance);

	@Inject
	protected CachedSearchEntryFactory entryFactory;

	@Inject
	protected SearchService() {}

	public SearchResult search(@NonNull String q) {
		validateQuery(q);
		var searchPattern = buildSearchPattern(q);
		var searcher = new Searcher(searchPattern);
		var matches = searcher.apply(entryFactory.createEntries()).sorted(RELEVANCE_COMPARATOR);
		return SearchResult.builder().entries(matches.toList()).build();
	}

	private void validateQuery(String q) {
		var trimmed = q.trim();
		try {
			Long.parseLong(trimmed);
			return;
		} catch (NumberFormatException e) {
			// Not a number
		}

		var nonSpace = SPLIT_PATTERN
				.splitAsStream(q)
				.filter(s -> !s.isBlank())
				.mapToInt(String::length)
				.sum();
		if (nonSpace < MIN_SEARCH_LENGTH) {
			throw new IllegalArgumentException(
					String.format("Query must contain at least %s non-whitespace characters", MIN_SEARCH_LENGTH));
		}
	}

	private Pattern buildSearchPattern(String q) {
		var queryParts = q.trim().split("\\s+");
		var regexString = String.join(".*", queryParts);
		return Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
	}
}
