package com.autonomouslogic.everef.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
	}
}
