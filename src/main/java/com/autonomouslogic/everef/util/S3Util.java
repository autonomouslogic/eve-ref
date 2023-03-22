package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.url.S3Url;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Singleton;

import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Utility for working with S3.
 */
@Singleton
public class S3Util {
	@Inject
	protected S3Util() {}

	public PutObjectRequest putObjectRequest(long len, S3Url url, String contentType) {
		return PutObjectRequest.builder()
				.bucket(url.getBucket())
				.key(url.getPath())
				.contentLength(len)
				.contentType(contentType)
				.build();
	}

	public PutObjectRequest putPublicObjectRequest(long len, S3Url url, String contentType) {
		return putObjectRequest(len, url, contentType)
				.toBuilder()
				.acl(ObjectCannedACL.PUBLIC_READ)
				.build();
	}

	public String cacheControl(Duration maxAge) {
		return String.format("public, max-age=%s", maxAge.getSeconds());
	}
}
