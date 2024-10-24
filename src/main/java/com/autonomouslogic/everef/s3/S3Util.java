package com.autonomouslogic.everef.s3;

import com.autonomouslogic.everef.url.S3Url;
import java.time.Duration;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Utility for working with S3.
 */
@Singleton
public class S3Util {
	@Inject
	protected S3Util() {}

	public PutObjectRequest putObjectRequest(long len, S3Url url) {
		return PutObjectRequest.builder()
				.bucket(url.getBucket())
				.key(url.getPath())
				.contentLength(len)
				.build();
	}

	public PutObjectRequest putObjectRequest(long len, S3Url url, String contentType) {
		return putObjectRequest(len, url).toBuilder().contentType(contentType).build();
	}

	public PutObjectRequest putObjectRequest(long len, S3Url url, String contentType, Duration maxAge) {
		return putObjectRequest(len, url, contentType).toBuilder()
				.cacheControl(cacheControl(maxAge))
				.build();
	}

	public PutObjectRequest putPublicObjectRequest(long len, S3Url url) {
		return putObjectRequest(len, url).toBuilder()
				.acl(ObjectCannedACL.PUBLIC_READ)
				.build();
	}

	public PutObjectRequest putPublicObjectRequest(long len, S3Url url, Duration maxAge) {
		return putPublicObjectRequest(len, url).toBuilder()
				.cacheControl(cacheControl(maxAge))
				.build();
	}

	public PutObjectRequest putPublicObjectRequest(long len, S3Url url, String contentType, Duration maxAge) {
		return putPublicObjectRequest(len, url, maxAge).toBuilder()
				.contentType(contentType)
				.build();
	}

	public DeleteObjectRequest deleteObjectRequest(S3Url url) {
		return DeleteObjectRequest.builder()
				.bucket(url.getBucket())
				.key(url.getPath())
				.build();
	}

	public String cacheControl(Duration maxAge) {
		return cacheControl(maxAge, false);
	}

	public String cacheControl(Duration maxAge, boolean immutable) {
		var parts = new ArrayList<String>();
		parts.add("public");
		parts.add("max-age=" + maxAge.toSeconds());
		if (immutable) {
			parts.add("immutable");
		}
		return String.join(", ", parts);
	}

	public GetObjectRequest getObjectRequest(S3Url url) {
		return GetObjectRequest.builder()
				.bucket(url.getBucket())
				.key(url.getPath())
				.build();
	}
}
