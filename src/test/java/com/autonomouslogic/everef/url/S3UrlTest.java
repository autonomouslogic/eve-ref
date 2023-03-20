package com.autonomouslogic.everef.url;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import lombok.AllArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class S3UrlTest {
	@AllArgsConstructor
	enum Tests {
		TEST_1("s3://bucket/key", "bucket", "key");
		String uri;
		String bucket;
		String path;
	}

	@ParameterizedTest
	@EnumSource(Tests.class)
	void shouldParseUrls(Tests test) {
		var url = S3Url.parse(URI.create(test.uri));
		assertEquals("s3", url.getProtocol());
		assertEquals(test.bucket, url.getBucket());
		assertEquals(test.path, url.getPath());
	}

	@ParameterizedTest
	@EnumSource(Tests.class)
	void shouldGenerateS3Urls(Tests test) {
		var obj = S3Url.builder().bucket(test.bucket).path(test.path).build();
		assertEquals(test.uri, obj.toString());
	}
}
