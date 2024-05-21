package com.autonomouslogic.everef.inject;

import com.autonomouslogic.commons.config.Config;
import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
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
	@Provides
	@Singleton
	@Named("data")
	public AwsCredentialsProvider dataCredentialsProvider() {
		return createProviderChain(
				Configs.DATA_AWS_ACCESS_KEY_ID, Configs.DATA_AWS_SECRET_ACCESS_KEY, Configs.DATA_AWS_PROFILE);
	}

	@Provides
	@Singleton
	@Named("data")
	public Optional<Region> dataRegion() {
		return Configs.DATA_AWS_REGION.get().map(Region::of);
	}

	@Provides
	@Singleton
	@Named("refdata")
	public AwsCredentialsProvider referenceDataCredentialsProvider() {
		return createProviderChain(
				Configs.REFERENCE_DATA_AWS_ACCESS_KEY_ID,
				Configs.REFERENCE_DATA_AWS_SECRET_ACCESS_KEY,
				Configs.REFERENCE_DATA_AWS_PROFILE);
	}

	@Provides
	@Singleton
	@Named("refdata")
	public Optional<Region> referenceDataRegion() {
		return Configs.REFERENCE_DATA_AWS_REGION.get().map(Region::of);
	}

	@Provides
	@Singleton
	@Named("dynamodb")
	public AwsCredentialsProvider dynamodbCredentialsProvider() {
		return createProviderChain(
				Configs.DYNAMODB_AWS_ACCESS_KEY_ID,
				Configs.DYNAMODB_AWS_SECRET_ACCESS_KEY,
				Configs.DYNAMODB_AWS_PROFILE);
	}

	@Provides
	@Singleton
	@Named("dynamodb")
	public Optional<Region> dynamodbRegion() {
		return Configs.DYNAMODB_AWS_REGION.get().map(Region::of);
	}

	private static AwsCredentialsProviderChain createProviderChain(
			Config<String> accessKeyConfig, Config<String> secretKeyConfig, Config<String> profileConfig) {
		var builder = AwsCredentialsProviderChain.builder();
		// Custom environment variables.
		var accessKey = accessKeyConfig.get();
		var secretKey = secretKeyConfig.get();
		if (accessKey.isPresent() && secretKey.isPresent()) {
			builder.addCredentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.get(), secretKey.get())));
		}
		// Default environment variables.
		builder.addCredentialsProvider(EnvironmentVariableCredentialsProvider.create());
		// Custom profile.
		profileConfig
				.get()
				.ifPresent(profile -> builder.addCredentialsProvider(ProfileCredentialsProvider.create(profile)));
		// Default profile.
		builder.addCredentialsProvider(ProfileCredentialsProvider.create());

		var chain = builder.build();
		return chain;
	}
}
