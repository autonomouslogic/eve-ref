package com.autonomouslogic.everef.data;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class FileEntry {
	String path;
	boolean directory;
	long size;
	Instant lastModified;
	String etag;

	public static FileEntry file(String path) {
		return new FileEntry(path, false, -1, null, null);
	}

	public static FileEntry file(String path, long size, Instant lastModified, String etag) {
		return new FileEntry(path, false, size, lastModified, etag);
	}

	public static FileEntry directory(String path) {
		return new FileEntry(path, true, 0, null, null);
	}
}
