package com.autonomouslogic.everef.url;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

public class S3UrlTest {
	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/url/S3UrlTest/s3-urls.csv")
	void shouldParseUrls(String uri, String bucket, String key) {
		var url = S3Url.parse(URI.create(uri));
		assertEquals("s3", url.getProtocol());
		assertEquals(bucket, url.getBucket());
		assertEquals(key, url.getPath());
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/url/S3UrlTest/s3-urls.csv")
	void shouldGenerateUrls(String uri, String bucket, String key) {
		var obj = S3Url.builder().bucket(bucket).path(key).build();
		assertEquals(uri, obj.toString());
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/url/S3UrlTest/s3-resolve.csv")
	void shouldResolveUrls(String base, String resolve, String expected) {
		var url = S3Url.parse(URI.create(base)).resolve(resolve);
		assertEquals(expected, url.toString());
	}
}
