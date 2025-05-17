package com.autonomouslogic.everef.inject;

import com.autonomouslogic.commons.config.Config;
import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.Setter;
import lombok.SneakyThrows;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Module
public class S3Module {
	@Setter
	S3AsyncClient dataClient;

	@Setter
	S3AsyncClient referenceDataClient;

	@Setter
	S3AsyncClient staticClient;

	@Provides
	@Named("data")
	@Singleton
	@SneakyThrows
	public S3AsyncClient dataClient(
			@Named("data") AwsCredentialsProvider credentialsProvider, @Named("data") Optional<Region> region) {
		if (dataClient != null) {
			return dataClient;
		}
		return createClient(credentialsProvider, region, Configs.DATA_S3_ENDPOINT_URL);
	}

	@Provides
	@Named("refdata")
	@Singleton
	@SneakyThrows
	public S3AsyncClient referenceDataClient(
			@Named("refdata") AwsCredentialsProvider credentialsProvider, @Named("refdata") Optional<Region> region) {
		if (referenceDataClient != null) {
			return referenceDataClient;
		}
		return createClient(credentialsProvider, region, Configs.REFERENCE_DATA_S3_ENDPOINT_URL);
	}

	@Provides
	@Named("static")
	@Singleton
	@SneakyThrows
	public S3AsyncClient staticClient(
			@Named("static") AwsCredentialsProvider credentialsProvider, @Named("static") Optional<Region> region) {
		if (staticClient != null) {
			return staticClient;
		}
		return createClient(credentialsProvider, region, Configs.STATIC_S3_ENDPOINT_URL);
	}

	private static S3AsyncClient createClient(
			AwsCredentialsProvider credentialsProvider, Optional<Region> region, Config<String> endpointUrl)
			throws URISyntaxException {
		var client = S3AsyncClient.builder().credentialsProvider(credentialsProvider);
		region.ifPresent(client::region);
		var endpoint = endpointUrl.get();
		if (endpoint.isPresent()) {
			client.endpointOverride(new URI(endpoint.get()));
		}
		client.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);
		return client.build();
	}
}
