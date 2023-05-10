package com.autonomouslogic.everef.url;

import java.net.URI;
import lombok.Builder;
import lombok.Value;

/**
 * URL for S3 data sources.
 */
@Value
@Builder(toBuilder = true)
public class HttpUrl implements DataUrl<HttpUrl> {
	URI uri;

	@Override
	public HttpUrl resolve(String path) {
		return parse(uri.resolve(path));
	}

	public String toString() {
		return uri.toString();
	}

	public String getProtocol() {
		return "http";
	}

	public String getHost() {
		return uri.getHost();
	}

	@Override
	public String getPath() {
		return uri.getPath();
	}

	@Override
	public URI toUri() {
		return uri;
	}

	static HttpUrl parse(URI url) {
		return HttpUrl.builder().uri(url).build();
	}
}
