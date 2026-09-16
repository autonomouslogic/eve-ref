package com.autonomouslogic.everef.inject;

import com.autonomouslogic.dynamomapper.DynamoAsyncMapper;
import dagger.Module;
import dagger.Provides;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import tools.jackson.databind.json.JsonMapper;

@Module
public class DynamoDBModule {
	@Provides
	@Singleton
	public DynamoDbAsyncClient dynamoDbAsyncClient(
			@Named("dynamodb") AwsCredentialsProvider credentialsProvider, @Named("dynamodb") Optional<Region> region) {
		var builder = DynamoDbAsyncClient.builder().credentialsProvider(credentialsProvider);
		region.ifPresent(builder::region);
		return builder.build();
	}

	@Provides
	@Singleton
	public DynamoAsyncMapper dynamoAsyncMapper(DynamoDbAsyncClient client, JsonMapper jsonMapper) {
		return DynamoAsyncMapper.builder().client(client).jsonMapper(jsonMapper).build();
	}
}
