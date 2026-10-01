package com.autonomouslogic.everef.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.inject.OkHttpModule;
import javax.inject.Provider;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_RATE_LIMIT_PER_S", value = "10")
class OkHttpModuleTest {
	private final OkHttpModule module = new OkHttpModule();
	private final EsiLimitExceededInterceptor limitExceededInterceptor = new EsiLimitExceededInterceptor();

	private OkHttpClient esiClient(Provider<EsiErrorLimitBudgetInterceptor> provider) {
		return module.esiHttpClient(
				null,
				new EsiUserAgentInterceptor(),
				new EsiRateLimitInterceptor(),
				new SocketErrorRetryInterceptor(),
				limitExceededInterceptor,
				new LoggingInterceptor(),
				provider);
	}

	@Test
	void configUnset_interceptorAbsent() {
		var client = esiClient(() -> {
			throw new AssertionError("should not be constructed");
		});
		assertFalse(client.interceptors().stream().anyMatch(i -> i instanceof EsiErrorLimitBudgetInterceptor));
	}

	@Test
	@SetEnvironmentVariable(key = "ESI_ERROR_LIMIT_MIN_REMAIN", value = "40")
	void configSet_interceptorPresentDirectlyAfterLimitExceeded() {
		var interceptor = new EsiErrorLimitBudgetInterceptor(40);
		var client = esiClient(() -> interceptor);
		var interceptors = client.interceptors();
		var limitIndex = interceptors.indexOf(limitExceededInterceptor);
		var errorLimitIndex = interceptors.indexOf(interceptor);
		assertTrue(limitIndex >= 0);
		assertTrue(errorLimitIndex == limitIndex + 1);
	}

	@Test
	@SetEnvironmentVariable(key = "ESI_ERROR_LIMIT_MIN_REMAIN", value = "40")
	void configSet_marketHistoryClientAlsoHasInterceptor() {
		var interceptor = new EsiErrorLimitBudgetInterceptor(40);
		var esiClient = esiClient(() -> interceptor);
		var marketHistoryInterceptor = new EsiMarketHistoryRateLimitExceededInterceptor();
		var marketClient = module.esiMarketHistoryHttpClient(esiClient, marketHistoryInterceptor);
		assertTrue(marketClient.interceptors().stream().anyMatch(i -> i instanceof EsiErrorLimitBudgetInterceptor));
	}

	@Test
	@SetEnvironmentVariable(key = "ESI_ERROR_LIMIT_MIN_REMAIN", value = "100")
	void configInvalid_esiHttpClientCreationFails() {
		assertThrows(IllegalArgumentException.class, () -> esiClient(EsiErrorLimitBudgetInterceptor::new));
	}
}
