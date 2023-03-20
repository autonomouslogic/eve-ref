package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
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
		// @todo support env vars
		// @todo support instance profile
		// @todo support custom profile
		var chain = AwsCredentialsProviderChain.builder()
				.addCredentialsProvider(ProfileCredentialsProvider.create("everef-data"));
		return chain.build();
	}

	@Provides
	@Singleton
	@Named("data")
	public Optional<Region> dataRegion() {
		return Configs.DATA_AWS_REGION.get().map(Region::of);
	}
}