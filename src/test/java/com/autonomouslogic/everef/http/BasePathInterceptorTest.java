package com.autonomouslogic.everef.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BasePathInterceptorTest {
	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		server = new MockWebServer();
		server.start();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.shutdown();
	}

	@SneakyThrows
	private OkHttpClient clientFor(String basePath) {
		var baseUrl = URI.create(server.url(basePath).toString());
		var interceptor = new BasePathInterceptor(baseUrl);
		return new OkHttpClient.Builder().addInterceptor(interceptor).build();
	}

	@SneakyThrows
	private int call(OkHttpClient client, String path) {
		server.enqueue(new MockResponse().setResponseCode(200));
		var request = new Request.Builder().url(server.url(path)).build();
		try (var response = client.newCall(request).execute()) {
			return response.code();
		}
	}

	@Test
	@SneakyThrows
	void allowsRequestExactlyAtBasePath() {
		var client = clientFor("/data");
		assertEquals(200, call(client, "/data"));
	}

	@Test
	@SneakyThrows
	void allowsRequestUnderBasePath() {
		var client = clientFor("/data");
		assertEquals(200, call(client, "/data/sub/file.json"));
	}

	@Test
	@SneakyThrows
	void allowsRequestUnderBasePathWithTrailingSlashConfigured() {
		var client = clientFor("/data/");
		assertEquals(200, call(client, "/data/sub/file.json"));
	}

	@Test
	@SneakyThrows
	void allowsAnyPathWhenBaseIsRoot() {
		var client = clientFor("/");
		assertEquals(200, call(client, "/anything"));
	}

	@Test
	@SneakyThrows
	void refusesSiblingPathThatOnlySharesAPrefix() {
		var client = clientFor("/data");
		var request = new Request.Builder().url(server.url("/data-other")).build();
		var thrown =
				assertThrows(IOException.class, () -> client.newCall(request).execute());
		assertEquals(true, thrown.getMessage().contains("Refusing request"));
	}

	@Test
	@SneakyThrows
	void refusesPathOutsideBasePath() {
		var client = clientFor("/data");
		var request = new Request.Builder().url(server.url("/other")).build();
		assertThrows(IOException.class, () -> client.newCall(request).execute());
	}

	@Test
	@SneakyThrows
	void refusesDifferentHost() {
		var baseUrl = URI.create("http://expected-host.invalid/data");
		var interceptor = new BasePathInterceptor(baseUrl);
		var client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
		var request = new Request.Builder().url(server.url("/data")).build();
		assertThrows(IOException.class, () -> client.newCall(request).execute());
	}
}
