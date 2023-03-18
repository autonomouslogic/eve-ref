package com.autonomouslogic.everef.test;

import static org.mockito.Mockito.mock;

import com.autonomouslogic.everef.s3.S3Adapter;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class MockS3Module {
	@Provides
	@Singleton
	public S3Adapter mockS3Adapter() {
		return mock(S3Adapter.class);
	}
}
