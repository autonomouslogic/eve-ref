package com.autonomouslogic.everef.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.net.URI;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class UrlParserTest {
	@Inject
	UrlParser urlParser;

	@BeforeEach
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	void shouldParseS3Urls() {
		var url = urlParser.parse("s3://bucket/key");
		assertSame(S3Url.class, url.getClass());
		var s3Url = (S3Url) url;
		assertEquals("s3", s3Url.getProtocol());
		assertEquals("bucket", s3Url.getBucket());
		assertEquals("key", s3Url.getPath());
		assertEquals(URI.create("s3://bucket/key"), s3Url.toUri());
	}

	@ParameterizedTest
	@ValueSource(strings = {"http", "https"})
	void shouldParseHttpUrls(String protocol) {
		var str = protocol + "://sub.example.com/some/path";
		var url = urlParser.parse(str);
		assertSame(HttpUrl.class, url.getClass());
		var httpUrl = (HttpUrl) url;
		assertEquals("http", httpUrl.getProtocol());
		assertEquals(str, httpUrl.toString());
		assertEquals("/some/path", httpUrl.getPath());
		assertEquals(URI.create(str), httpUrl.toUri());
	}
}
