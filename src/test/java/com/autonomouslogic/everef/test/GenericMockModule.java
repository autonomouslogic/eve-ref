package com.autonomouslogic.everef.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.autonomouslogic.everef.util.DataIndexHelper;
import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Singleton;

@Module
public class GenericMockModule {
	@Provides
	@Singleton
	public DataIndexHelper dataIndexHelper() {
		var obj = mock(DataIndexHelper.class);
		lenient().when(obj.updateIndex(any())).thenReturn(Completable.complete());
		lenient().when(obj.updateIndex(any(), any())).thenReturn(Completable.complete());
		return obj;
	}
}
