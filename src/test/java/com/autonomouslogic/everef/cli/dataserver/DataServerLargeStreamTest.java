package com.autonomouslogic.everef.cli.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import mockwebserver3.MockResponse;
import mockwebserver3.MockResponseBody;
import mockwebserver3.MockWebServer;
import okio.BufferedSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Proves the data-server streams a multi-GB object with constant memory, never buffering it, by downloading
 * ~1 GiB against a small fixed heap (see the {@code slowTest} Gradle task). A buffering bug causes an OOM
 * well before the download finishes.
 */
@Tag("slow")
@SetEnvironmentVariable(key = "HTTP_PORT", value = "" + DataServerLargeStreamTest.DATA_SERVER_TEST_PORT)
@SetEnvironmentVariable(
		key = "DATA_SERVER_ORIGIN_URL",
		value = "http://localhost:" + DataServerLargeStreamTest.UPSTREAM_PORT + "/bucket/")
@Log4j2
@Timeout(300)
public class DataServerLargeStreamTest {
	public static final int DATA_SERVER_TEST_PORT = 29220;
	public static final int UPSTREAM_PORT = 29221;
	private static final long LARGE_STREAM_SIZE = 1L * 1024 * 1024 * 1024;
	private static final int CHUNK_SIZE = 64 * 1024;

	@Inject
	DataServer dataServer;

	MockWebServer upstream;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		upstream = new MockWebServer();
		upstream.start(UPSTREAM_PORT);
		dataServer.startServer();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		dataServer.stop();
		upstream.close();
	}

	private static byte[] generateChunk() {
		var chunk = new byte[CHUNK_SIZE];
		new Random(42).nextBytes(chunk);
		return chunk;
	}

	@SneakyThrows
	private static String expectedSha256() {
		var md = MessageDigest.getInstance("SHA-256");
		var chunk = generateChunk();
		long remaining = LARGE_STREAM_SIZE;
		while (remaining > 0) {
			int n = (int) Math.min(chunk.length, remaining);
			md.update(chunk, 0, n);
			remaining -= n;
		}
		return HexFormat.of().formatHex(md.digest());
	}

	@Test
	@SneakyThrows
	void shouldStreamLargeFileWithConstantMemory() {
		upstream.enqueue(new MockResponse.Builder()
				.code(200)
				.setHeader("ETag", "\"big\"")
				.setHeader("x-amz-version-id", "v1")
				.body(new MockResponseBody() {
					@Override
					public long getContentLength() {
						return LARGE_STREAM_SIZE;
					}

					@Override
					public void writeTo(BufferedSink sink) throws java.io.IOException {
						var chunk = generateChunk();
						long remaining = LARGE_STREAM_SIZE;
						while (remaining > 0) {
							int n = (int) Math.min(chunk.length, remaining);
							sink.write(chunk, 0, n);
							remaining -= n;
						}
					}
				})
				.build());

		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + DATA_SERVER_TEST_PORT + "/big.bin"))
				.GET()
				.build();
		var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
		assertEquals(200, response.statusCode());

		var md = MessageDigest.getInstance("SHA-256");
		long total = 0;
		try (var in = response.body()) {
			var buffer = new byte[CHUNK_SIZE];
			int n;
			while ((n = in.read(buffer)) != -1) {
				md.update(buffer, 0, n);
				total += n;
			}
		}
		assertEquals(LARGE_STREAM_SIZE, total);
		assertEquals(expectedSha256(), HexFormat.of().formatHex(md.digest()));
	}
}
