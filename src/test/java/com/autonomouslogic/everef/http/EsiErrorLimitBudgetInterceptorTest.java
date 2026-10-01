package com.autonomouslogic.everef.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import lombok.SneakyThrows;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(30)
class EsiErrorLimitBudgetInterceptorTest {
	MockWebServer server;
	OkHttpClient client;
	EsiErrorLimitBudgetInterceptor interceptor;

	@BeforeEach
	@SneakyThrows
	void setup() {
		server = new MockWebServer();
		server.start();
		interceptor = new EsiErrorLimitBudgetInterceptor(10);
		client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.shutdown();
	}

	private MockResponse response(int code, Integer remain, Integer reset) {
		var r = new MockResponse().setResponseCode(code);
		if (remain != null) {
			r.setHeader(EsiErrorLimitHeaders.REMAIN_HEADER, remain);
		}
		if (reset != null) {
			r.setHeader(EsiErrorLimitHeaders.RESET_TIME_HEADER, reset);
		}
		return r;
	}

	@SneakyThrows
	private void call() {
		call("/");
	}

	@SneakyThrows
	private void call(String path) {
		var request = new Request.Builder().url(server.url(path)).build();
		client.newCall(request).execute().close();
	}

	@Test
	@SneakyThrows
	void remainAboveThreshold_doesNotBlock() {
		server.enqueue(response(200, 50, 5));
		server.enqueue(response(200, 50, 5));
		call();
		call();
		assertEquals(Instant.MIN, interceptor.currentBlockedUntil());
	}

	@Test
	@SneakyThrows
	void remainAtOrBelowThreshold_blocksNextRequest() {
		server.enqueue(response(200, 10, 1));
		server.enqueue(response(200, 50, 5));

		call();
		var start = Instant.now();
		call();
		var elapsed = Duration.between(start, Instant.now());

		assertTrue(elapsed.compareTo(Duration.ofMillis(800)) >= 0, "expected a wait of roughly 2s, took " + elapsed);
		assertTrue(elapsed.compareTo(Duration.ofSeconds(5)) < 0, "expected a wait of roughly 2s, took " + elapsed);
	}

	@Test
	@SneakyThrows
	void noErrorLimitHeaders_ignored() {
		server.enqueue(new MockResponse().setResponseCode(200));
		call();
		assertEquals(Instant.MIN, interceptor.currentBlockedUntil());
	}

	@ParameterizedTest
	@CsvSource({"true,false", "false,true"})
	@SneakyThrows
	void onlyOneHeaderPresent_ignored(boolean hasRemain, boolean hasReset) {
		server.enqueue(response(200, hasRemain ? 3 : null, hasReset ? 5 : null));
		call();
		assertEquals(Instant.MIN, interceptor.currentBlockedUntil());
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc", "-1", ""})
	@SneakyThrows
	void unparseableRemain_ignored(String value) {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader(EsiErrorLimitHeaders.REMAIN_HEADER, value)
				.setHeader(EsiErrorLimitHeaders.RESET_TIME_HEADER, 5));
		call();
		assertEquals(Instant.MIN, interceptor.currentBlockedUntil());
	}

	@Test
	@SneakyThrows
	void resetAbove120_capped() {
		server.enqueue(response(200, 0, 300));
		var before = Instant.now();
		call();
		var after = Instant.now();
		var blockedUntil = interceptor.currentBlockedUntil();
		assertTrue(blockedUntil.compareTo(before.plusSeconds(120)) >= 0);
		assertTrue(blockedUntil.compareTo(after.plusSeconds(120)) <= 0);
	}

	@Test
	@SneakyThrows
	void cacheHitWithStaleLowRemainHeaders_ignored(@TempDir File tempDir) {
		var cache = new Cache(tempDir, 1024 * 1024);
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Cache-Control", "max-age=3600")
				.setHeader(EsiErrorLimitHeaders.REMAIN_HEADER, 3)
				.setHeader(EsiErrorLimitHeaders.RESET_TIME_HEADER, 1));
		var cachingClient = new OkHttpClient.Builder()
				.cache(cache)
				.addInterceptor(interceptor)
				.build();
		var request = new Request.Builder().url(server.url("/cached")).build();
		cachingClient.newCall(request).execute().close();
		var afterFirst = interceptor.currentBlockedUntil();

		cachingClient.newCall(request).execute().close();
		assertEquals(afterFirst, interceptor.currentBlockedUntil());
		assertEquals(1, server.getRequestCount());
	}

	@Test
	@SneakyThrows
	void revalidation304_usesFreshNetworkHeaders(@TempDir File tempDir) {
		var cache = new Cache(tempDir, 1024 * 1024);
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Cache-Control", "max-age=0")
				.setHeader("ETag", "\"v1\"")
				.setHeader(EsiErrorLimitHeaders.REMAIN_HEADER, 50)
				.setHeader(EsiErrorLimitHeaders.RESET_TIME_HEADER, 5));
		server.enqueue(new MockResponse()
				.setResponseCode(304)
				.setHeader(EsiErrorLimitHeaders.REMAIN_HEADER, 3)
				.setHeader(EsiErrorLimitHeaders.RESET_TIME_HEADER, 1));
		var cachingClient = new OkHttpClient.Builder()
				.cache(cache)
				.addInterceptor(interceptor)
				.build();
		var request = new Request.Builder().url(server.url("/etag")).build();
		cachingClient.newCall(request).execute().close();
		assertEquals(Instant.MIN, interceptor.currentBlockedUntil());

		cachingClient.newCall(request).execute().close();
		assertFalse(interceptor.currentBlockedUntil().equals(Instant.MIN));
	}

	@Test
	@SneakyThrows
	void interruptedWhileWaiting_throwsInterruptedIOException() {
		server.enqueue(response(200, 0, 30));
		call();

		var caughtInterruptedIOException = new java.util.concurrent.atomic.AtomicBoolean(false);
		var stillInterrupted = new java.util.concurrent.atomic.AtomicBoolean(false);
		var thread = new Thread(() -> {
			try {
				call("/next");
			} catch (Exception e) {
				caughtInterruptedIOException.set(
						e instanceof IOException && e.getCause() instanceof java.io.InterruptedIOException
								|| e instanceof java.io.InterruptedIOException);
				stillInterrupted.set(Thread.currentThread().isInterrupted());
			}
		});
		thread.start();
		Thread.sleep(200);
		thread.interrupt();
		thread.join();

		assertTrue(caughtInterruptedIOException.get());
		assertTrue(stillInterrupted.get());
	}

	@Test
	void constructorRejectsInvalidThreshold() {
		assertThrows(IllegalArgumentException.class, () -> new EsiErrorLimitBudgetInterceptor(-1));
		assertThrows(IllegalArgumentException.class, () -> new EsiErrorLimitBudgetInterceptor(100));
	}
}
