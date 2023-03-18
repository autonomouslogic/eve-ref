package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import java.net.URI;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Module
public class S3Module {
	@Provides
	@Named("data")
	@Singleton
	@SneakyThrows
	public S3AsyncClient dataClient(
			@Named("data") AwsCredentialsProvider credentialsProvider, @Named("data") Region region) {
		var client =
				S3AsyncClient.builder().credentialsProvider(credentialsProvider).region(region);
		var endpoint = Configs.DATA_S3_ENDPOINT.get();
		if (endpoint.isPresent()) {
			client.endpointOverride(new URI(endpoint.get()));
		}
		return client.build();
	}
}
