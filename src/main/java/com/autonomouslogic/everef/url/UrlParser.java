package com.autonomouslogic.everef.url;

import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UrlParser {
	@Inject
	protected UrlParser() {}

	public DataUrl parse(String url) {
		return parse(URI.create(url));
	}

	public DataUrl parse(URI url) {
		switch (url.getScheme()) {
			case "s3":
				return S3Url.parse(url);
			default:
				throw new RuntimeException("Unknown protocol: " + url.getScheme());
		}
	}

	private String leftTrim(String s, String c) {
		while (s.startsWith(c)) {
			s = s.substring(1);
		}
		return s;
	}

	private String normaliseSlashes(String s) {
		return s.replaceAll("/+", "/");
	}
}
