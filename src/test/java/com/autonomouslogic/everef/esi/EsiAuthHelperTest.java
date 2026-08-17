package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonomouslogic.dynamomapper.DynamoAsyncMapper;
import com.autonomouslogic.everef.inject.JacksonModule;
import com.autonomouslogic.everef.model.CharacterLogin;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

@SetEnvironmentVariable(key = "EVE_OAUTH_CLIENT_ID", value = "test-client-id")
@SetEnvironmentVariable(key = "EVE_OAUTH_SECRET_KEY", value = "test-secret-key")
@Timeout(10)
public class EsiAuthHelperTest {
	static final String TABLE_NAME = "everef-logins";
	static final CharacterLogin CHARACTER_LOGIN = CharacterLogin.builder()
			.characterOwnerHash("test-owner-hash")
			.characterId(12345L)
			.characterName("Test Character")
			.refreshToken("my-refresh-token")
			.scopes(List.of("esi-universe.read_structures.v1"))
			.build();

	DynamoDbAsyncClient dynamoClient;
	EsiAuthHelper esiAuthHelper;

	@BeforeEach
	@SneakyThrows
	void setup() {
		dynamoClient = Mockito.mock(DynamoDbAsyncClient.class);
		var objectMapper = new JacksonModule().objectMapper();
		var dynamoAsyncMapper = DynamoAsyncMapper.builder()
				.client(dynamoClient)
				.objectMapper(objectMapper)
				.build();
		esiAuthHelper = new EsiAuthHelper();
		var field = EsiAuthHelper.class.getDeclaredField("dynamoAsyncMapper");
		field.setAccessible(true);
		field.set(esiAuthHelper, dynamoAsyncMapper);
	}

	@Test
	@SneakyThrows
	void putCharacterLoginUsesCorrectTable() {
		when(dynamoClient.putItem(any(PutItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						PutItemResponse.builder().build()));

		esiAuthHelper.putCharacterLogin(CHARACTER_LOGIN).blockingAwait();

		var captor = ArgumentCaptor.forClass(PutItemRequest.class);
		verify(dynamoClient).putItem(captor.capture());
		assertEquals(TABLE_NAME, captor.getValue().tableName());
	}

	@Test
	@SneakyThrows
	void putCharacterLoginEncodesAttributesCorrectly() {
		when(dynamoClient.putItem(any(PutItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						PutItemResponse.builder().build()));

		esiAuthHelper.putCharacterLogin(CHARACTER_LOGIN).blockingAwait();

		var captor = ArgumentCaptor.forClass(PutItemRequest.class);
		verify(dynamoClient).putItem(captor.capture());
		var item = captor.getValue().item();
		assertEquals(AttributeValue.fromS("test-owner-hash"), item.get("character_owner_hash"));
		assertEquals(AttributeValue.fromN("12345"), item.get("character_id"));
		assertEquals(AttributeValue.fromS("Test Character"), item.get("character_name"));
		assertEquals(AttributeValue.fromS("my-refresh-token"), item.get("refresh_token"));
		assertEquals(
				AttributeValue.fromL(List.of(AttributeValue.fromS("esi-universe.read_structures.v1"))),
				item.get("scopes"));
	}

	@Test
	@SneakyThrows
	void getCharacterLoginUsesCorrectTable() {
		when(dynamoClient.getItem(any(GetItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						GetItemResponse.builder().item(Map.of()).build()));

		esiAuthHelper.getCharacterLogin("test-owner-hash");

		var captor = ArgumentCaptor.forClass(GetItemRequest.class);
		verify(dynamoClient).getItem(captor.capture());
		assertEquals(TABLE_NAME, captor.getValue().tableName());
	}

	@Test
	@SneakyThrows
	void getCharacterLoginUsesPrimaryKeyAttribute() {
		when(dynamoClient.getItem(any(GetItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						GetItemResponse.builder().item(Map.of()).build()));

		esiAuthHelper.getCharacterLogin("test-owner-hash");

		var captor = ArgumentCaptor.forClass(GetItemRequest.class);
		verify(dynamoClient).getItem(captor.capture());
		assertEquals(
				Map.of("character_owner_hash", AttributeValue.fromS("test-owner-hash")),
				captor.getValue().key());
	}

	@Test
	@SneakyThrows
	void getCharacterLoginDecodesItemFromDynamoResponse() {
		var item = Map.of(
				"character_owner_hash", AttributeValue.fromS("test-owner-hash"),
				"character_id", AttributeValue.fromN("12345"),
				"character_name", AttributeValue.fromS("Test Character"),
				"refresh_token", AttributeValue.fromS("my-refresh-token"),
				"scopes", AttributeValue.fromL(List.of(AttributeValue.fromS("esi-universe.read_structures.v1"))));
		when(dynamoClient.getItem(any(GetItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						GetItemResponse.builder().item(item).build()));

		var result = esiAuthHelper.getCharacterLogin("test-owner-hash");

		assertTrue(result.isPresent());
		assertEquals(CHARACTER_LOGIN, result.get());
	}

	@Test
	@SneakyThrows
	void getCharacterLoginReturnsEmptyWhenItemNotFound() {
		when(dynamoClient.getItem(any(GetItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						GetItemResponse.builder().build()));

		var result = esiAuthHelper.getCharacterLogin("nonexistent-hash");

		assertTrue(result.isEmpty());
	}
}
