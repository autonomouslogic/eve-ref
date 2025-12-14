package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.model.api.search.SearchEntry;
import com.autonomouslogic.everef.model.api.search.SearchResult;
import com.autonomouslogic.everef.model.api.search.Searcher;
import com.autonomouslogic.everef.service.RefDataService;
import com.google.common.collect.Ordering;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class SearchService {
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

	private static final Comparator<SearchEntry> RELEVANCE_COMPARATOR =
			Ordering.natural().onResultOf(SearchEntry::getRelevance);

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected InventoryTypeSearchEntryFactory inventoryTypeSearchEntryFactory;

	@Inject
	protected MarketGroupSearchEntryFactory marketGroupSearchEntryFactory;

	@Inject
	protected CategorySearchEntryFactory categorySearchEntryFactory;

	@Inject
	protected GroupSearchEntryFactory groupSearchEntryFactory;

	@Inject
	public SearchService() {}

	public SearchResult search(@NonNull String q) {
		validateQuery(q);
		var searchPattern = buildSearchPattern(q);
		var searcher = new Searcher(searchPattern);
		var matches = searcher.apply(getSearchEntries()).sorted(RELEVANCE_COMPARATOR);
		return SearchResult.builder().entries(matches.toList()).build();
	}

	private Stream<SearchEntry> getSearchEntries() {
		return Stream.of(
						inventoryTypeSearchEntryFactory.createEntries(),
						marketGroupSearchEntryFactory.createEntries(),
						categorySearchEntryFactory.createEntries(),
						groupSearchEntryFactory.createEntries())
				.flatMap(stream -> stream);
	}

	private void validateQuery(String q) {
		var nonSpace = SPLIT_PATTERN
				.splitAsStream(q)
				.filter(s -> !s.isBlank())
				.mapToInt(String::length)
				.sum();
		if (nonSpace < 3) {
			throw new IllegalArgumentException("Query must contain at least three non-whitespace characters");
		}
	}

	private Pattern buildSearchPattern(String q) {
		var queryParts = q.trim().split("\\s+");
		var regexString = String.join(".*", queryParts);
		return Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
	}
}
