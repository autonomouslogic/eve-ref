package com.autonomouslogic.everef.cli.publiccontracts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.reactivex.rxjava3.core.Flowable;
import io.sentry.Sentry;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unit tests for {@link ContractAbyssalFetcher} focused on Sentry reporting.
 * Tests call {@code apply()} directly on the test thread so that Mockito's static mocking
 * intercepts the Sentry calls (mockStatic is ThreadLocal and does not propagate to virtual threads).
 */
@ExtendWith(MockitoExtension.class)
@Timeout(30)
@Log4j2
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "REF_DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
public class ContractAbyssalFetcherTest {
	private static final int TYPE_ID = 47804;
	private static final long ITEM_ID = 1800001L;
	private static final long CONTRACT_ID = 1800L;
	private static final String META_GROUPS_JSON =
			"{\"meta_group_id\":15,\"type_ids\":[" + TYPE_ID + "]}";
	private static final String TYPE_JSON =
			"{\"type_id\":" + TYPE_ID + ",\"name\":\"Test\",\"description\":\"\",\"group_id\":1,\"published\":true}";

	@Inject
	ContractAbyssalFetcher fetcher;

	Map<Long, JsonNode> dynamicItemsStore;
	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		server = new MockWebServer();
		server.start(TestDataUtil.TEST_PORT);
		dynamicItemsStore = new HashMap<>();
		fetcher.setDynamicItemsStore(dynamicItemsStore);
		fetcher.setDogmaAttributesStore(new HashMap<>());
		fetcher.setDogmaEffectsStore(new HashMap<>());
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	/**
	 * When the dogma/dynamic ESI endpoint returns a failed status code, the failure is reported
	 * to Sentry at WARNING level with the status code in the message and contract_id, item_id,
	 * type_id as extras.
	 */
	@ParameterizedTest
	@ValueSource(ints = {400, 404, 500, 520})
	@SneakyThrows
	void failedStatusCodeReportedToSentry(int statusCode) {
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest request) {
				var path = request.getRequestUrl().encodedPath();
				if (path.startsWith("/universe/types/")) {
					return mockJson(TYPE_JSON);
				}
				if (path.equals("/meta_groups/15")) {
					return mockJson(META_GROUPS_JSON);
				}
				if (path.startsWith("/dogma/dynamic/items/") || path.startsWith("/latest/dogma/dynamic/items/")) {
					return new MockResponse().setResponseCode(statusCode);
				}
				return new MockResponse().setResponseCode(404);
			}
		});

		try (var sentryMock = Mockito.mockStatic(Sentry.class)) {
			fetcher.apply(CONTRACT_ID, Flowable.just(buildItem())).blockingAwait();

			sentryMock.verify(() -> Sentry.captureException(
					argThat(e -> e instanceof RuntimeException
							&& e.getMessage().contains(String.valueOf(statusCode))),
					any(io.sentry.ScopeCallback.class)));
		}
	}

	private static final String DYNAMIC_ITEM_JSON = """
		{"created_by":203457312,"mutator_type_id":47801,"source_type_id":31928,\
		"dogma_attributes":[{"attribute_id":277,"value":1}],\
		"dogma_effects":[{"effect_id":16,"is_default":false}]}""";

	/**
	 * Successful 200 response saves the dynamic item and does not report to Sentry.
	 */
	@Test
	@SneakyThrows
	void successfulFetchDoesNotReportToSentry() {
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest request) {
				var path = request.getRequestUrl().encodedPath();
				if (path.startsWith("/universe/types/")) {
					return mockJson(TYPE_JSON);
				}
				if (path.equals("/meta_groups/15")) {
					return mockJson(META_GROUPS_JSON);
				}
				if (path.startsWith("/dogma/dynamic/items/") || path.startsWith("/latest/dogma/dynamic/items/")) {
					return mockJson(DYNAMIC_ITEM_JSON);
				}
				return new MockResponse().setResponseCode(404);
			}
		});

		try (var sentryMock = Mockito.mockStatic(Sentry.class)) {
			fetcher.apply(CONTRACT_ID, Flowable.just(buildItem())).blockingAwait();

			sentryMock.verifyNoInteractions();
			assertFalse(dynamicItemsStore.isEmpty(), "Dynamic item must be saved on successful fetch");
		}
	}

	private com.fasterxml.jackson.databind.node.ObjectNode buildItem() {
		return JsonNodeFactory.instance
				.objectNode()
				.put("contract_id", CONTRACT_ID)
				.put("item_id", ITEM_ID)
				.put("type_id", TYPE_ID)
				.put("is_included", true)
				.put("quantity", 1);
	}

	@NotNull
	private MockResponse mockJson(String body) {
		return new MockResponse()
				.setResponseCode(200)
				.setBody(body)
				.addHeader("last-modified", "Mon, 03 Apr 2023 03:47:30 GMT");
	}
}
