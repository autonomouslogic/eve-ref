package com.autonomouslogic.everef.cli;

import static org.mockito.Mockito.when;

import dagger.MembersInjector;
import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Singleton;
import lombok.Setter;
import org.mockito.Mockito;

/**
 * Specifically for overriding DataIndex, when needed.
 */
@Module
public class MockDataIndexModule {
	@Setter
	private DataIndex dataIndex;

	@Setter
	private boolean defaultMock = false;

	@Provides
	@Singleton
	public DataIndex dataIndex(MembersInjector<DataIndex> injector) {
		if (dataIndex != null) {
			return dataIndex;
		}
		if (defaultMock) {
			var mock = Mockito.mock(DataIndex.class);
			when(mock.run()).thenReturn(Completable.complete());
			return mock;
		}
		// Fall back to the real thing.
		var dataIndex = new DataIndex();
		injector.injectMembers(dataIndex);
		return dataIndex;
	}
}
