package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for WarsFetchScope, covering war ID fetching and filtering logic.
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
public class WarsFetchScopeTest {
	private ObjectMapper objectMapper;
	private Map<Long, JsonNode> warsMap;

	@Mock
	private OkHttpWrapper mockOkHttpWrapper;

	private WarsFetchScope warsFetchScope;

	@Inject
	protected WarsFetchScopeTest() {}

	@BeforeEach
	void setup() {
		objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
		warsMap = new HashMap<>();
		warsFetchScope = new WarsFetchScope();
		// Set injected dependencies using reflection or direct field access
		try {
			var field = WarsFetchScope.class.getDeclaredField("okHttpWrapper");
			field.setAccessible(true);
			field.set(warsFetchScope, mockOkHttpWrapper);

			field = WarsFetchScope.class.getDeclaredField("objectMapper");
			field.setAccessible(true);
			field.set(warsFetchScope, objectMapper);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@SneakyThrows
	void shouldFetchAllWarIds() {
		mockResponse("[1000, 1001, 1002]", "1");

		var scope = warsFetchScope.calculate(warsMap);

		assertEquals(3, scope.getWarIds().size());
		assertEquals(1002L, scope.getMaxWarId());
	}

	@Test
	@SneakyThrows
	void shouldIdentifyUnfinishedWars() {
		mockResponse("[1000, 1001]", "1");

		// Add unfinished war
		var unfinishedWar = createMockWar(1000L, null);
		warsMap.put(1000L, unfinishedWar);

		var scope = warsFetchScope.calculate(warsMap);

		// Both wars should be in scope (1 unfinished + 1 unknown)
		assertEquals(2, scope.getWarIds().size());
		assertEquals(1, scope.getUnfinishedCount());
		assertEquals(1, scope.getUnknownCount());
	}

	@Test
	@SneakyThrows
	void shouldIgnoreFinishedWars() {
		mockResponse("[1002, 1003]", "1");

		// Add finished war
		var finishedWar = createMockWar(1000L, Instant.now().toString());
		warsMap.put(1000L, finishedWar);

		var scope = warsFetchScope.calculate(warsMap);

		// Scope includes existing war (1000) + new wars from API (1002, 1003)
		assertEquals(3, scope.getWarIds().size());
		assertTrue(scope.getWarIds().contains(1000L));
		assertTrue(scope.getWarIds().contains(1002L));
		assertTrue(scope.getWarIds().contains(1003L));
		assertEquals(0, scope.getUnfinishedCount());
		assertEquals(2, scope.getUnknownCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleEmptyWarsMap() {
		mockResponse("[1000, 1001, 1002]", "1");

		var scope = warsFetchScope.calculate(warsMap);

		// All wars are unknown
		assertEquals(3, scope.getWarIds().size());
		assertEquals(0, scope.getUnfinishedCount());
		assertEquals(3, scope.getUnknownCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleEmptyWarIdSet() {
		mockResponse("[]", "1");

		var scope = warsFetchScope.calculate(warsMap);

		assertEquals(0, scope.getWarIds().size());
		assertEquals(0, scope.getMaxWarId());
	}

	@Test
	@SneakyThrows
	void shouldCalculateMaxWarId() {
		mockResponse("[100, 2000, 1500, 500]", "1");

		var scope = warsFetchScope.calculate(warsMap);

		assertEquals(2000L, scope.getMaxWarId());
	}

	@SneakyThrows
	private void mockResponse(String body, String pages) {
		var mockResponse = mock(okhttp3.Response.class);
		when(mockResponse.code()).thenReturn(200);
		when(mockResponse.header("X-Pages")).thenReturn(pages);

		var mockResponseBody = ResponseBody.create(body, MediaType.get("application/json"));
		when(mockResponse.body()).thenReturn(mockResponseBody);

		when(mockOkHttpWrapper.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(mockResponse);
	}

	/**
	 * Creates a mock war JsonNode.
	 */
	private com.fasterxml.jackson.databind.node.ObjectNode createMockWar(long warId, String finishedTime) {
		var war = objectMapper.createObjectNode();
		war.put("id", warId);
		war.put("declared", Instant.parse("2020-01-01T00:00:00Z").toString());

		if (finishedTime != null) {
			war.put("finished", finishedTime);
		}

		return war;
	}
}
