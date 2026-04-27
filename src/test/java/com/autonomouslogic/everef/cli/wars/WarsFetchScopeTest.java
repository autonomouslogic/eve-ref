package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for WarsFetchScope, covering war ID fetching and filtering logic.
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SuppressWarnings("unchecked")
public class WarsFetchScopeTest {
	private ObjectMapper objectMapper;
	private Map<Long, JsonNode> warsMap;

	@Inject
	protected WarsFetchScopeTest() {}

	@BeforeEach
	void setup() {
		objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
		warsMap = new HashMap<>();
	}

	@Test
	@SneakyThrows
	void shouldFetchAllWarIds() {
		var mockWarsApi = mock(WarsApi.class);
		var mockEsiHelper = mock(EsiHelper.class);

		var allWarIds = java.util.List.of(1000L, 1001L, 1002L);
		when(mockEsiHelper.fetchPages(org.mockito.ArgumentMatchers.any())).thenReturn((java.util.List) allWarIds);

		var scope = WarsFetchScope.calculate(mockWarsApi, mockEsiHelper, warsMap);

		assertEquals(3, scope.getWarIds().size());
		assertEquals(1002L, scope.getMaxWarId());
	}

	@Test
	@SneakyThrows
	void shouldIdentifyUnfinishedWars() {
		var mockWarsApi = mock(WarsApi.class);
		var mockEsiHelper = mock(EsiHelper.class);

		var allWarIds = java.util.List.of(1000L, 1001L);
		when(mockEsiHelper.fetchPages(org.mockito.ArgumentMatchers.any())).thenReturn((java.util.List) allWarIds);

		// Add unfinished war
		var unfinishedWar = createMockWar(1000L, null);
		warsMap.put(1000L, unfinishedWar);

		var scope = WarsFetchScope.calculate(mockWarsApi, mockEsiHelper, warsMap);

		// Both wars should be in scope (1 unfinished + 1 unknown)
		assertEquals(2, scope.getWarIds().size());
		assertEquals(1, scope.getUnfinishedCount());
		assertEquals(1, scope.getUnknownCount());
	}

	@Test
	@SneakyThrows
	void shouldIgnoreFinishedWars() {
		var mockWarsApi = mock(WarsApi.class);
		var mockEsiHelper = mock(EsiHelper.class);

		var allWarIds = java.util.List.of(1000L, 1001L);
		when(mockEsiHelper.fetchPages(org.mockito.ArgumentMatchers.any())).thenReturn((java.util.List) allWarIds);

		// Add finished war
		var finishedWar = createMockWar(1000L, Instant.now().toString());
		warsMap.put(1000L, finishedWar);

		var scope = WarsFetchScope.calculate(mockWarsApi, mockEsiHelper, warsMap);

		// Only unknown war should be in scope
		assertEquals(1, scope.getWarIds().size());
		assertTrue(scope.getWarIds().contains(1001L));
		assertEquals(0, scope.getUnfinishedCount());
		assertEquals(1, scope.getUnknownCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleEmptyWarsMap() {
		var mockWarsApi = mock(WarsApi.class);
		var mockEsiHelper = mock(EsiHelper.class);

		var allWarIds = java.util.List.of(1000L, 1001L, 1002L);
		when(mockEsiHelper.fetchPages(org.mockito.ArgumentMatchers.any())).thenReturn((java.util.List) allWarIds);

		var scope = WarsFetchScope.calculate(mockWarsApi, mockEsiHelper, warsMap);

		// All wars are unknown
		assertEquals(3, scope.getWarIds().size());
		assertEquals(0, scope.getUnfinishedCount());
		assertEquals(3, scope.getUnknownCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleEmptyWarIdSet() {
		var mockWarsApi = mock(WarsApi.class);
		var mockEsiHelper = mock(EsiHelper.class);

		when(mockEsiHelper.fetchPages(any())).thenReturn(java.util.List.of());

		var scope = WarsFetchScope.calculate(mockWarsApi, mockEsiHelper, warsMap);

		assertEquals(0, scope.getWarIds().size());
		assertEquals(0, scope.getMaxWarId());
	}

	@Test
	@SneakyThrows
	void shouldCalculateMaxWarId() {
		var mockWarsApi = mock(WarsApi.class);
		var mockEsiHelper = mock(EsiHelper.class);

		var allWarIds = java.util.List.of(100L, 2000L, 1500L, 500L);
		when(mockEsiHelper.fetchPages(org.mockito.ArgumentMatchers.any())).thenReturn((java.util.List) allWarIds);

		var scope = WarsFetchScope.calculate(mockWarsApi, mockEsiHelper, warsMap);

		assertEquals(2000L, scope.getMaxWarId());
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
