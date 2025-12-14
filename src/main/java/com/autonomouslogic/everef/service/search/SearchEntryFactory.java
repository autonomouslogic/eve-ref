package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.model.api.search.SearchEntry;
import java.util.stream.Stream;

public interface SearchEntryFactory {
	Stream<SearchEntry> createEntries();
}
