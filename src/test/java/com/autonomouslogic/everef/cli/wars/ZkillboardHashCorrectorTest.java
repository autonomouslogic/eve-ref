package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for ZkillboardHashCorrector using MockWebServer to simulate Zkillboard responses.
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
public class ZkillboardHashCorrectorTest {
	private ObjectMapper objectMapper;
	private ZkillboardHashCorrector corrector;
	private MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

		corrector = new ZkillboardHashCorrector();
		corrector.objectMapper = objectMapper;
		corrector.okHttpWrapper = new OkHttpWrapper(new okhttp3.OkHttpClient());

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldFindCorrectedHashInZkillboard() {
		var result = corrector.correctHash(1000L, "wrong_hash");

		// Should find the hash in Zkillboard data
		assertTrue(result.isPresent());
		assertEquals("correct_hash_123", result.get());
	}

	@Test
	@SneakyThrows
	void shouldSkipCCPVerifiedKillmails() {
		var result = corrector.correctHash(2000L, "wrong_hash");

		// Should return empty for CCP VERIFIED killmails
		assertFalse(result.isPresent());
	}

	@Test
	@SneakyThrows
	void shouldReturnEmptyIfNotFound() {
		var result = corrector.correctHash(9999L, "wrong_hash");

		// Should return empty if killmail not found
		assertFalse(result.isPresent());
	}

	@Test
	@SneakyThrows
	void shouldHandleNetworkErrors() {
		// Server will return 500 for invalid dates
		var result = corrector.correctHash(3000L, "wrong_hash");

		// Should handle errors gracefully
		assertFalse(result.isPresent());
	}

	/**
	 * Mock dispatcher that returns Zkillboard data.
	 */
	private class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			var path = request.getRequestUrl().encodedPath();

			// Match Zkillboard history endpoint
			if (path.matches("/history/\\d{8}\\.json")) {
				try {
					var json = objectMapper.createObjectNode();
					json.put("1000", "correct_hash_123");
					json.put("2000", "CCP VERIFIED");
					json.put("3000", "another_hash");

					return new MockResponse().setBody(objectMapper.writeValueAsString(json));
				} catch (Exception e) {
					log.error("Error in dispatcher", e);
					return new MockResponse().setResponseCode(500);
				}
			}

			return new MockResponse().setResponseCode(404);
		}
	}
}
