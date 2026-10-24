package com.autonomouslogic.everef.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.autonomouslogic.everef.url.S3Url;
import dagger.MembersInjector;
import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import javax.inject.Singleton;
import lombok.Setter;
import org.mockito.Mockito;

/**
 * Specifically for overriding DataIndexHelper, when needed. Defaults to a mock, so S3 index files aren't actually
 * built/uploaded unless explicitly asked for.
 */
@Module
public class MockDataIndexHelperModule {
	@Setter
	private DataIndexHelper dataIndexHelper;

	@Setter
	private boolean defaultMock = true;

	@Provides
	@Singleton
	public DataIndexHelper dataIndexHelper(MembersInjector<DataIndexHelper> injector) {
		if (dataIndexHelper != null) {
			return dataIndexHelper;
		}
		if (defaultMock) {
			var mock = Mockito.mock(DataIndexHelper.class);
			lenient().when(mock.updateIndex(any(List.class))).thenReturn(Completable.complete());
			lenient().when(mock.updateIndex(any(S3Url.class))).thenReturn(Completable.complete());
			lenient().when(mock.updateIndex(any(S3Url.class), any(S3Url.class))).thenReturn(Completable.complete());
			return mock;
		}
		// Fall back to the real thing.
		var dataIndexHelper = new DataIndexHelper();
		injector.injectMembers(dataIndexHelper);
		return dataIndexHelper;
	}
}
