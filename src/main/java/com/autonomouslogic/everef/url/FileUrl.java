package com.autonomouslogic.everef.url;

import java.net.URI;
import lombok.Builder;
import lombok.Value;

/**
 * URL for S3 data sources.
 */
@Value
@Builder(toBuilder = true)
public class FileUrl implements DataUrl<FileUrl> {
	String path;

	@Override
	public FileUrl resolve(String path) {
		return parse(toUri().resolve(path));
	}

	public String toString() {
		return String.format("file://%s", path);
	}

	public String getProtocol() {
		return "file";
	}

	static FileUrl parse(URI url) {
		var path = url.getPath();
		return FileUrl.builder().path(path).build();
	}
}
