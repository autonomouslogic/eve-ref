package com.autonomouslogic.everef.test;

import static org.mockito.Mockito.mock;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.s3.S3Adapter;
import dagger.Module;
import dagger.Provides;
import java.net.URI;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
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
		// Tests that exercise the data server point the client at a MockWebServer via DATA_S3_ENDPOINT_URL.
		var endpoint = Configs.DATA_S3_ENDPOINT_URL.get();
		if (endpoint.isEmpty()) {
			return mock(S3AsyncClient.class);
		}
		return S3AsyncClient.builder()
				.endpointOverride(URI.create(endpoint.get()))
				.httpClient(NettyNioAsyncHttpClient.builder()
						.maxConcurrency(Configs.DATA_SERVER_MAX_CONCURRENCY.getRequired())
						.build())
				.region(Region.US_EAST_1)
				.forcePathStyle(true)
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
				// Retries would consume additional queued mock responses.
				.overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry()))
				.build();
	}

	@Provides
	@Named("refdata")
	@Singleton
	@SneakyThrows
	public S3AsyncClient refDataClient() {
		return mock(S3AsyncClient.class);
	}

	@Provides
	@Named("static")
	@Singleton
	@SneakyThrows
	public S3AsyncClient staticClient() {
		return mock(S3AsyncClient.class);
	}
}
