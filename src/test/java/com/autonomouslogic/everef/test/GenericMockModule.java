package com.autonomouslogic.everef.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import javax.inject.Singleton;

@Module
public class GenericMockModule {
	@Provides
	@Singleton
	public DataIndexHelper dataIndexHelper() {
		var obj = mock(DataIndexHelper.class);
		lenient().when(obj.updateIndex(any(List.class))).thenReturn(Completable.complete());
		lenient().when(obj.updateIndex(any(S3Url.class))).thenReturn(Completable.complete());
		lenient().when(obj.updateIndex(any(S3Url.class), any(S3Url.class))).thenReturn(Completable.complete());
		return obj;
	}
}
