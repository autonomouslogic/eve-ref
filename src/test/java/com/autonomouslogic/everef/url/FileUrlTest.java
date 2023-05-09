package com.autonomouslogic.everef.url;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

public class FileUrlTest {
	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/url/FileUrlTest/file-urls.csv")
	void shouldParseUrls(String uri, String path) {
		var url = FileUrl.parse(URI.create(uri));
		assertEquals("file", url.getProtocol());
		assertEquals(path, url.getPath());
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/url/FileUrlTest/file-resolve.csv")
	void shouldResolveUrls(String base, String resolve, String expected) {
		var url = FileUrl.parse(URI.create(base)).resolve(resolve);
		assertEquals(expected, url.toString());
	}
}
