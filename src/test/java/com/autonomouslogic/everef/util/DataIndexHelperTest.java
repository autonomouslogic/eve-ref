package com.autonomouslogic.everef.util;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.cli.DataIndex;
import com.autonomouslogic.everef.url.S3Url;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DataIndexHelperTest {
	@Module
	static class TestModule {
		@Provides
		@Singleton
		public DataIndex dataIndex() {
			var obj = mock(DataIndex.class);
			when(obj.setRecursive(anyBoolean())).thenReturn(obj);
			when(obj.setPrefix(anyString())).thenReturn(obj);
			when(obj.run()).thenReturn(Completable.complete().subscribeOn(Schedulers.computation()));
			return obj;
		}
	}

	@Component(modules = {TestModule.class})
	@Singleton
	public interface TestComponent {
		void inject(DataIndexHelperTest obj);
	}

	@Inject
	DataIndex dataIndex;

	@Inject
	DataIndexHelper dataIndexHelper;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerDataIndexHelperTest_TestComponent.builder().build().inject(this);
	}

	@Test
	void shouldUpdateIndexes() {
		dataIndexHelper
				.updateIndex(
						S3Url.builder()
								.bucket("bucket")
								.path("market-orders/market-orders-latest.v3.csv.bz2")
								.build(),
						S3Url.builder()
								.bucket("bucket")
								.path(
										"market-orders/history/2023/2023-09-11/market-orders-2023-09-11_05-47-09.v3.csv.bz2")
								.build())
				.blockingAwait();
		var inOrder = Mockito.inOrder(dataIndex);

		inOrder.verify(dataIndex).setPrefix("market-orders/");
		inOrder.verify(dataIndex).setRecursive(false);
		inOrder.verify(dataIndex).run();

		inOrder.verify(dataIndex).setPrefix("");
		inOrder.verify(dataIndex).setRecursive(false);
		inOrder.verify(dataIndex).run();

		inOrder.verify(dataIndex).setPrefix("market-orders/history/2023/2023-09-11/");
		inOrder.verify(dataIndex).setRecursive(false);
		inOrder.verify(dataIndex).run();

		inOrder.verify(dataIndex).setPrefix("market-orders/history/2023/");
		inOrder.verify(dataIndex).setRecursive(false);
		inOrder.verify(dataIndex).run();

		inOrder.verify(dataIndex).setPrefix("market-orders/history/");
		inOrder.verify(dataIndex).setRecursive(false);
		inOrder.verify(dataIndex).run();

		inOrder.verifyNoMoreInteractions();
	}
}
