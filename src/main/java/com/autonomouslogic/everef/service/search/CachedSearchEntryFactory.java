package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.model.api.search.SearchEntry;
import com.autonomouslogic.everef.service.RefDataService;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class CachedSearchEntryFactory implements SearchEntryFactory {
	@NonNull
	private final SearchEntryFactory delegate;

	@NonNull
	private final RefDataService refDataService;

	private List<SearchEntry> cached;
	private int lastCode = 0;

	@Override
	public Stream<SearchEntry> createEntries() {
		var hashCode = System.identityHashCode(refDataService.getLoadedRefData());
		if (!isCacheValid(hashCode)) {
			log.debug("Creating cached search entries");
			cached = delegate.createEntries().toList();
			lastCode = hashCode;
		}
		return cached.stream();
	}

	private boolean isCacheValid(int hashCode) {
		if (cached == null) {
			return false;
		}
		return lastCode == hashCode;
	}
}
