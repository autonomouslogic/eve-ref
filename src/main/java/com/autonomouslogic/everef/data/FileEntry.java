package com.autonomouslogic.everef.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class FileEntry {
	String path;
	boolean directory;
	long size;
	String md5Hex;

	public static FileEntry file(String path, long size, String md5Hex) {
		return new FileEntry(path, false, size, md5Hex);
	}

	public static FileEntry directory(String path) {
		return new FileEntry(path, true, 0, null);
	}
}
