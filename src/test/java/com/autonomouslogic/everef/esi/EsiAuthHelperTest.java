package com.autonomouslogic.everef.esi;

import static com.autonomouslogic.everef.esi.EsiAuthHelperTest.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
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
@SetEnvironmentVariable(
		key = "EVE_OAUTH_AUTHORIZATION_URL",
		value = "http://localhost:" + TEST_PORT + "/v2/oauth/authorize")
@SetEnvironmentVariable(key = "EVE_OAUTH_TOKEN_URL", value = "http://localhost:" + TEST_PORT + "/v2/oauth/token")
@SetEnvironmentVariable(key = "OAUTH_CALLBACK_URL", value = "http://localhost:" + TEST_PORT + "/callback")
@Timeout(10)
public class EsiAuthHelperTest {
	static final int TEST_PORT = 30151;
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
	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		dynamoClient = Mockito.mock(DynamoDbAsyncClient.class);
		var objectMapper = new JacksonModule().jsonMapper();
		var dynamoAsyncMapper = DynamoAsyncMapper.builder()
				.client(dynamoClient)
				.jsonMapper(objectMapper)
				.build();
		server = new MockWebServer();
		server.start(TEST_PORT);

		esiAuthHelper = new EsiAuthHelper();
		var dynamoField = EsiAuthHelper.class.getDeclaredField("dynamoAsyncMapper");
		dynamoField.setAccessible(true);
		dynamoField.set(esiAuthHelper, dynamoAsyncMapper);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
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

	@Test
	@SneakyThrows
	void refreshAccessTokenReturnsTokenOnSuccess() {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Content-Type", "application/json")
				.setBody("""
						{"access_token":"new-access-token","token_type":"Bearer","expires_in":1200,"refresh_token":"new-refresh-token"}
						"""));

		var token = esiAuthHelper.refreshAccessToken("my-refresh-token");

		assertEquals("new-access-token", token.getAccessToken());
		assertEquals("new-refresh-token", token.getRefreshToken());
		assertEquals(1200, token.getExpiresIn());

		var request = server.takeRequest();
		assertTrue(request.getBody().readUtf8().contains("refresh_token=my-refresh-token"));
	}

	@Test
	@SneakyThrows
	void refreshAccessTokenThrowsOnServerError() {
		for (int i = 0; i < 3; i++) {
			server.enqueue(new MockResponse().setResponseCode(504).setBody("""
							<html><head><title>504 Gateway Time-out</title></head><body><center><h1>504 Gateway Time-out</h1></center></body></html>
							"""));
		}

		assertThrows(ExecutionException.class, () -> esiAuthHelper.refreshAccessToken("my-refresh-token"));
		assertEquals(3, server.getRequestCount());
	}

	@Test
	@SneakyThrows
	void refreshAccessTokenRetriesOnServerError() {
		server.enqueue(new MockResponse().setResponseCode(504).setBody("""
						<html><head><title>504 Gateway Time-out</title></head><body><center><h1>504 Gateway Time-out</h1></center></body></html>
						"""));
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Content-Type", "application/json")
				.setBody("""
						{"access_token":"new-access-token","token_type":"Bearer","expires_in":1200,"refresh_token":"new-refresh-token"}
						"""));

		var token = esiAuthHelper.refreshAccessToken("my-refresh-token");

		assertEquals("new-access-token", token.getAccessToken());
		assertEquals(2, server.getRequestCount());
	}

	@Test
	@SneakyThrows
	void getTokenForOwnerHashReturnsEmptyWhenLoginNotFound() {
		when(dynamoClient.getItem(any(GetItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						GetItemResponse.builder().build()));

		var result = esiAuthHelper.getTokenForOwnerHash("test-owner-hash");

		assertTrue(result.isEmpty());
		assertEquals(0, server.getRequestCount());
	}

	@Test
	@SneakyThrows
	void getTokenForOwnerHashRefreshesTokenForFoundLogin() {
		var item = Map.of(
				"character_owner_hash", AttributeValue.fromS("test-owner-hash"),
				"character_id", AttributeValue.fromN("12345"),
				"character_name", AttributeValue.fromS("Test Character"),
				"refresh_token", AttributeValue.fromS("my-refresh-token"),
				"scopes", AttributeValue.fromL(List.of(AttributeValue.fromS("esi-universe.read_structures.v1"))));
		when(dynamoClient.getItem(any(GetItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						GetItemResponse.builder().item(item).build()));
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Content-Type", "application/json")
				.setBody("""
						{"access_token":"new-access-token","token_type":"Bearer","expires_in":1200,"refresh_token":"new-refresh-token"}
						"""));

		var result = esiAuthHelper.getTokenForOwnerHash("test-owner-hash");

		assertTrue(result.isPresent());
		assertEquals("new-access-token", result.get().getAccessToken());
		var request = server.takeRequest();
		assertTrue(request.getBody().readUtf8().contains("refresh_token=my-refresh-token"));
	}

	@Test
	@SneakyThrows
	void getTokenForOwnerHashUsesCacheOnSubsequentCalls() {
		var item = Map.of(
				"character_owner_hash", AttributeValue.fromS("test-owner-hash"),
				"character_id", AttributeValue.fromN("12345"),
				"character_name", AttributeValue.fromS("Test Character"),
				"refresh_token", AttributeValue.fromS("my-refresh-token"),
				"scopes", AttributeValue.fromL(List.of(AttributeValue.fromS("esi-universe.read_structures.v1"))));
		when(dynamoClient.getItem(any(GetItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						GetItemResponse.builder().item(item).build()));
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Content-Type", "application/json")
				.setBody("""
						{"access_token":"new-access-token","token_type":"Bearer","expires_in":1200,"refresh_token":"new-refresh-token"}
						"""));

		var first = esiAuthHelper.getTokenForOwnerHash("test-owner-hash");
		var second = esiAuthHelper.getTokenForOwnerHash("test-owner-hash");

		assertEquals(first, second);
		verify(dynamoClient).getItem(any(GetItemRequest.class));
		assertEquals(1, server.getRequestCount());
	}

	@Test
	@SneakyThrows
	void getTokenForOwnerHashRefreshesWhenCacheExpired() {
		var item = Map.of(
				"character_owner_hash", AttributeValue.fromS("test-owner-hash"),
				"character_id", AttributeValue.fromN("12345"),
				"character_name", AttributeValue.fromS("Test Character"),
				"refresh_token", AttributeValue.fromS("my-refresh-token"),
				"scopes", AttributeValue.fromL(List.of(AttributeValue.fromS("esi-universe.read_structures.v1"))));
		when(dynamoClient.getItem(any(GetItemRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(
						GetItemResponse.builder().item(item).build()));
		// expires_in shorter than EXPIRATION_BUFFER (1 minute), so the cached entry is already expired.
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Content-Type", "application/json")
				.setBody("""
						{"access_token":"first-access-token","token_type":"Bearer","expires_in":30,"refresh_token":"my-refresh-token"}
						"""));
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Content-Type", "application/json")
				.setBody("""
						{"access_token":"second-access-token","token_type":"Bearer","expires_in":1200,"refresh_token":"my-refresh-token"}
						"""));

		var first = esiAuthHelper.getTokenForOwnerHash("test-owner-hash");
		var second = esiAuthHelper.getTokenForOwnerHash("test-owner-hash");

		assertEquals("first-access-token", first.get().getAccessToken());
		assertEquals("second-access-token", second.get().getAccessToken());
		verify(dynamoClient, Mockito.times(2)).getItem(any(GetItemRequest.class));
		assertEquals(2, server.getRequestCount());
	}
}
