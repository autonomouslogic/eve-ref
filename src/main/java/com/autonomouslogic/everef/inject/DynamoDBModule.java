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
	public DynamoAsyncMapper dynamoAsyncMapper(DynamoDbAsyncClient client) {
		// dynamo-mapper requires Jackson 2 ObjectMapper (com.fasterxml.jackson.databind.ObjectMapper),
		// not Jackson 3 JsonMapper (tools.jackson.databind.json.JsonMapper).
		var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
		return DynamoAsyncMapper.builder()
				.client(client)
				.objectMapper(objectMapper)
				.build();
	}
}
