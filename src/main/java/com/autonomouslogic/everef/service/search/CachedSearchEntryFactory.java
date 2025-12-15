package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.model.api.search.SearchEntry;
import com.autonomouslogic.everef.service.RefDataService;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class CachedSearchEntryFactory implements SearchEntryFactory {
	@Inject
	protected RefDataService refDataService;

	@Inject
	protected CompoundSearchEntryFactory entryFactory;

	private volatile List<SearchEntry> cached;
	private final ReentrantReadWriteLock.WriteLock cacheLock = new ReentrantReadWriteLock().writeLock();
	private volatile int lastCode = 0;

	@Inject
	public CachedSearchEntryFactory() {}

	@Override
	public Stream<SearchEntry> createEntries() {
		var hashCode = System.identityHashCode(refDataService.getLoadedRefData());
		if (!isCacheValid(hashCode)) {
			try {
				cacheLock.lock();
				if (!isCacheValid(hashCode)) {
					log.debug("Creating cached search entries");
					cached = entryFactory.createEntries().toList();
					lastCode = hashCode;
				}
			} finally {
				cacheLock.unlock();
			}
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
