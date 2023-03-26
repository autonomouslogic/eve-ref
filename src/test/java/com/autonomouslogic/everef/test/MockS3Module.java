package com.autonomouslogic.everef.test;

import static org.mockito.Mockito.mock;

import com.autonomouslogic.everef.s3.S3Adapter;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Module
@Log4j2
public class MockS3Module {
	@Provides
	@Singleton
	public MockS3Adapter mockS3Adapter() {
		return new MockS3Adapter();
	}

	@Provides
	@Singleton
	public S3Adapter s3Adapter(MockS3Adapter mock) {
		return mock;
	}

	@Provides
	@Named("data")
	@Singleton
	@SneakyThrows
	public S3AsyncClient dataClient() {
		return mock(S3AsyncClient.class);
	}
}
