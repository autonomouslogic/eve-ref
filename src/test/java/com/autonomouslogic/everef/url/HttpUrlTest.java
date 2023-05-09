package com.autonomouslogic.everef.url;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

public class HttpUrlTest {
	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/url/HttpUrlTest/http-urls.csv")
	void shouldParseUrls(String uri, String host, String path) {
		var url = HttpUrl.parse(URI.create(uri));
		assertEquals("http", url.getProtocol());
		assertEquals(host, url.getHost());
		assertEquals(path, url.getPath());
	}
}
