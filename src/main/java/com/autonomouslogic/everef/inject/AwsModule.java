package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * DI for AWS.
 */
@Module
@Singleton
public class AwsModule {
	@Provides
	@Singleton
	@Named("data")
	public AwsCredentialsProvider awsCredentialsProvider() {
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
	public Region dataRegion() {
		return Region.of(Configs.DATA_S3_REGION.getRequired());
	}
}
