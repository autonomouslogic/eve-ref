package com.autonomouslogic.everef.cli.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.dataserver.StallWatchdog;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockResponseBody;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Proves that many concurrent slow readers don't become a capacity problem: a fast request stays fast while
 * hundreds of other streams are open, and the stall watchdog cleans every stream up once readers stop. See
 * data-server-plan.md, "Concurrency and slow readers".
 */
@Tag("slow")
@SetEnvironmentVariable(key = "HTTP_PORT", value = "" + DataServerConcurrencyTest.DATA_SERVER_TEST_PORT)
@SetEnvironmentVariable(
		key = "DATA_SERVER_ORIGIN_URL",
		value = "http://localhost:" + DataServerConcurrencyTest.UPSTREAM_PORT + "/bucket/")
@Log4j2
@Timeout(300)
public class DataServerConcurrencyTest {
	public static final int DATA_SERVER_TEST_PORT = 29222;
	public static final int UPSTREAM_PORT = 29223;
	private static final int SLOW_READER_COUNT = 500;
	private static final long SLOW_BODY_SIZE = 1024 * 1024; // 1 MiB per slow stream
	private static final int SMALL_BODY_SIZE = 64;

	@Inject
	DataServer dataServer;

	@Inject
	StallWatchdog watchdog;

	MockWebServer upstream;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		upstream = new MockWebServer();
		upstream.setDispatcher(new PathBasedDispatcher());
		upstream.start(UPSTREAM_PORT);
		dataServer.startServer();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		dataServer.stop();
		upstream.close();
	}

	@Test
	@SneakyThrows
	void shouldServeFastRequestsWhileManySlowReadersAreOpenThenCleanUp() {
		var executor = Executors.newVirtualThreadPerTaskExecutor();
		var httpClient = HttpClient.newHttpClient();
		List<Future<Long>> slowDownloads = new ArrayList<>();
		for (int i = 0; i < SLOW_READER_COUNT; i++) {
			slowDownloads.add(executor.submit(() -> downloadSlowly(httpClient)));
		}

		// Give the slow readers time to all open their connections.
		Thread.sleep(500);

		var start = System.nanoTime();
		var smallResponse = httpClient.send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + DATA_SERVER_TEST_PORT + "/small.txt"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofByteArray());
		var elapsedMs = (System.nanoTime() - start) / 1_000_000;
		assertEquals(200, smallResponse.statusCode());
		assertEquals(SMALL_BODY_SIZE, smallResponse.body().length);
		assertTrue(elapsedMs < 1000, "small request took " + elapsedMs + "ms while slow readers were open");

		for (var future : slowDownloads) {
			assertEquals(SLOW_BODY_SIZE, future.get(120, TimeUnit.SECONDS));
		}
		executor.shutdown();
		assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

		assertEquals(0, watchdog.activeStreamCount());
	}

	@SneakyThrows
	private long downloadSlowly(HttpClient httpClient) {
		var response = httpClient.send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + DATA_SERVER_TEST_PORT + "/slow.bin"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofInputStream());
		long total = 0;
		try (var in = response.body()) {
			var buffer = new byte[8 * 1024];
			int n;
			while ((n = in.read(buffer)) != -1) {
				total += n;
				Thread.sleep(5);
			}
		}
		return total;
	}

	private static byte[] generateBytes(int size) {
		var bytes = new byte[size];
		new java.util.Random(7).nextBytes(bytes);
		return bytes;
	}

	private class PathBasedDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(RecordedRequest request) {
			if (request.getTarget().contains("/small")) {
				return new MockResponse.Builder()
						.code(200)
						.setHeader("ETag", "\"small\"")
						.setHeader("x-amz-version-id", "v-small")
						.body(new okio.Buffer().write(generateBytes(SMALL_BODY_SIZE)))
						.build();
			}
			return new MockResponse.Builder()
					.code(200)
					.setHeader("ETag", "\"slow\"")
					.setHeader("x-amz-version-id", "v-slow")
					.body(new MockResponseBody() {
						@Override
						public long getContentLength() {
							return SLOW_BODY_SIZE;
						}

						@Override
						public void writeTo(BufferedSink sink) throws java.io.IOException {
							var chunk = generateBytes(64 * 1024);
							long remaining = SLOW_BODY_SIZE;
							while (remaining > 0) {
								int n = (int) Math.min(chunk.length, remaining);
								sink.write(chunk, 0, n);
								remaining -= n;
							}
						}
					})
					.build();
		}
	}
}
