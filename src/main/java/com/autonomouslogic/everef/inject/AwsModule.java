package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * DI for AWS.
 */
@Module
public class AwsModule {
	@Setter
	AwsCredentialsProvider dataCredentialsProvider;

	@Provides
	@Singleton
	@Named("data")
	public AwsCredentialsProvider awsCredentialsProvider() {
		if (dataCredentialsProvider != null) {
			return dataCredentialsProvider;
		}
		var builder = AwsCredentialsProviderChain.builder();
		// Custom environment variables.
		var accessKey = Configs.DATA_AWS_ACCESS_KEY_ID.get();
		var secretKey = Configs.DATA_AWS_SECRET_ACCESS_KEY.get();
		if (accessKey.isPresent() && secretKey.isPresent()) {
			builder.addCredentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.get(), secretKey.get())));
		}
		// Default environment variables.
		builder.addCredentialsProvider(EnvironmentVariableCredentialsProvider.create());
		// Custom profile.
		Configs.DATA_AWS_PROFILE
				.get()
				.ifPresent(profile -> builder.addCredentialsProvider(ProfileCredentialsProvider.create(profile)));
		// Default profile.
		builder.addCredentialsProvider(ProfileCredentialsProvider.create());

		var chain = builder.build();
		return chain;
	}

	@Provides
	@Singleton
	@Named("data")
	public Optional<Region> dataRegion() {
		return Configs.DATA_AWS_REGION.get().map(Region::of);
	}
}
