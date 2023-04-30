package com.autonomouslogic.everef.http;

import dagger.MembersInjector;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import lombok.Setter;

/**
 * Specifically for overriding DataIndex, when needed.
 */
@Module
public class MockDataCrawlerModule {
	@Setter
	private DataCrawler dataCrawler;

	@Setter
	private boolean defaultMock = false;

	@Provides
	@Singleton
	public DataCrawler dataCrawler(MembersInjector<DataCrawler> injector) {
		if (dataCrawler != null) {
			return dataCrawler;
		}
		// Fall back to the real thing.
		var dataCrawler = new DataCrawler();
		injector.injectMembers(dataCrawler);
		return dataCrawler;
	}
}
