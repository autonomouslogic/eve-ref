package com.autonomouslogic.everef.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.TempFiles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OkHttpHelperTest {
	TempFiles tempFiles = new TempFiles();
	OkHttpClient client;
	OkHttpHelper helper;
	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		client = new OkHttpClient();
		helper = new OkHttpHelper();
		server = new MockWebServer();
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.shutdown();
	}

	@Test
	@SneakyThrows
	void shouldDownloadFiles() {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setBody("content\n")
				.addHeader("Last-Modified", "Mon, 06 Jan 2020 00:07:14 GMT"));
		var file = tempFiles
				.tempFile(OkHttpHelperTest.class.getSimpleName(), "test")
				.toFile();
		file.delete();
		var url = String.format("http://localhost:%s/test", server.getPort());

		helper.download(url, file, client).ignoreElement().blockingAwait();
		assertEquals("content\n", IOUtils.toString(file.toURI(), StandardCharsets.UTF_8));
		assertEquals(
				Instant.parse("2020-01-06T00:07:14Z"),
				Files.getLastModifiedTime(file.toPath()).toInstant());

		var req = server.takeRequest();
		assertEquals("GET", req.getMethod());
		assertEquals(url, req.getRequestUrl().toString());
	}
}
