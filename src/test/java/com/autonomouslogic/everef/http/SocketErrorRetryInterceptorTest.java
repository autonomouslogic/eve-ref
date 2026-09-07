package com.autonomouslogic.everef.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SocketErrorRetryInterceptorTest {
	MockWebServer server;
	OkHttpClient client;

	@BeforeEach
	@SneakyThrows
	void setup() {
		server = new MockWebServer();
		server.start();
		// 0ms delays so tests run fast.
		var interceptor = new SocketErrorRetryInterceptor(0, 0, 1.0);
		client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.shutdown();
	}

	/**
	 * Demonstrates the bug: IOException("Canceled") is not retried even though it originates from an
	 * OkHttp call timeout. The interceptor's isCanceledFromTimeout() looks for a timeout cause in
	 * the exception's cause chain, but at interceptor time the cause chain is not yet populated by
	 * OkHttp — that wrapping happens after the interceptor chain exits.
	 */
	@Test
	@SneakyThrows
	void canceledExceptionShouldBeRetried() {
		var callCount = new AtomicInteger(0);

		// Inner interceptor simulates what RetryAndFollowUpInterceptor does on timeout:
		// throws IOException("Canceled") with no cause chain (exactly as OkHttp does it).
		var throwingInterceptor = (okhttp3.Interceptor) chain -> {
			int attempt = callCount.incrementAndGet();
			if (attempt < 3) {
				throw new IOException("Canceled");
			}
			return chain.proceed(chain.request());
		};

		var testClient = client.newBuilder().addInterceptor(throwingInterceptor).build();
		server.enqueue(new MockResponse().setResponseCode(200));

		var request = new Request.Builder().url(server.url("/test")).build();
		try (var response = testClient.newCall(request).execute()) {
			assertEquals(200, response.code());
		}

		// SocketErrorRetryInterceptor should have retried, resulting in 3 total attempts.
		assertEquals(3, callCount.get());
	}
}
