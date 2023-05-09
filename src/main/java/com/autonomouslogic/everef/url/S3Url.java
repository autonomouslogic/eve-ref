package com.autonomouslogic.everef.url;

import java.net.URI;
import lombok.Builder;
import lombok.Value;

/**
 * URL for S3 data sources.
 */
@Value
@Builder(toBuilder = true)
public class S3Url implements DataUrl<S3Url> {
	String bucket;
	String path;

	@Override
	public S3Url resolve(String path) {
		return null;
	}

	public String toString() {
		return String.format("s3://%s/%s", bucket, path);
	}

	public String getProtocol() {
		return "s3";
	}

	static S3Url parse(URI url) {
		var path = url.getPath();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return S3Url.builder().bucket(url.getHost()).path(path).build();
	}
}
