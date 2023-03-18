package com.autonomouslogic.everef.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Singleton;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Utility for working with S3.
 */
@Singleton
public class S3Util {
	public static final Duration ULTRA_SHORT_CACHE = Duration.ofMinutes(1);
	public static final Duration SHORT_CACHE = Duration.ofMinutes(5);
	public static final Duration LONG_CACHE = Duration.ofDays(1);

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected S3Util() {}

	public PutObjectRequest putObjectRequest(InputStream in, long len, URL s3Uri, String contentType) {
		// @todo create URL parser util.
		var bucket = s3Uri.getHost();
		var key = s3Uri.getPath().substring(1);
		return PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentLength(len)
				.contentType(contentType)
				.build();
	}

	public String cacheControl(Duration maxAge) {
		return String.format("public, max-age=%s", maxAge.getSeconds());
	}
}
