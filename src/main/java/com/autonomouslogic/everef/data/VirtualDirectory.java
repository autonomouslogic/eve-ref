package com.autonomouslogic.everef.data;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides a virtual file listing in memory.
 */
@RequiredArgsConstructor
public class VirtualDirectory {
	private final Dir root = new Dir(FileEntry.directory(""));

	public VirtualDirectory add(@NonNull FileEntry file) {
		file = clean(file);
		var parentPath = FilenameUtils.getPath(file.getPath());
		var name = FilenameUtils.getName(file.getPath());
		var dir = traverse(parentPath, true);
		dir.files.put(name, file);
		return this;
	}

	private FileEntry clean(@NonNull FileEntry file) {
		if (file.getPath().startsWith("/")) {
			file = file.toBuilder()
					.path(StringUtils.removeStart(file.getPath(), "/"))
					.build();
		}
		if (file.getPath().endsWith("/")) {
			file = file.toBuilder()
					.path(StringUtils.removeEnd(file.getPath(), "/"))
					.build();
		}
		return file;
	}

	private String removeSlashes(@NonNull String path) {
		path = StringUtils.removeStart(path, "/");
		path = StringUtils.removeEnd(path, "/");
		return path;
	}

	public boolean exists(@NonNull String path) {
		path = removeSlashes(path);
		var parentPath = FilenameUtils.getPath(path);
		var name = FilenameUtils.getName(path);
		var dir = traverse(parentPath, false);
		if (dir == null) {
			return false;
		}
		return dir.dirs.containsKey(name) || dir.files.containsKey(name);
	}

	private Dir traverse(@NonNull String path, boolean create) {
		path = removeSlashes(path);
		if (path.isEmpty()) {
			return root;
		}
		var segments = path.split("\\/");
		var current = root;
		var traversed = "";
		for (String segment : segments) {
			if (!traversed.isEmpty()) {
				traversed += "/";
			}
			traversed += segment;
			if (current.dirs.containsKey(segment)) {
				current = current.dirs.get(segment);
			} else if (create) {
				var dir = new Dir(FileEntry.directory(traversed));
				current.dirs.put(segment, dir);
				current = dir;
			} else {
				return null;
			}
		}
		return current;
	}

	public Stream<FileEntry> list(boolean recursive) {
		return list(root, recursive);
	}

	public Stream<FileEntry> list(@NonNull String path, boolean recursive) {
		var dir = traverse(path, false);
		if (dir == null) {
			return null;
		}
		return list(dir, recursive);
	}

	private Stream<FileEntry> list(@NonNull Dir dir, boolean recursive) {
		var dirEntries = Stream.of(dir.self);
		if (recursive) {
			dirEntries = Stream.concat(dirEntries, dir.dirs.values().stream().flatMap(d -> list(d, true)));
		} else {
			dirEntries = Stream.concat(dirEntries, dir.dirs.values().stream().map(d -> d.self));
		}
		var files = dir.files.values().stream();
		return Stream.concat(dirEntries, files);
	}

	@RequiredArgsConstructor
	@ToString(onlyExplicitlyIncluded = true)
	private static class Dir {
		@ToString.Include
		final FileEntry self;

		final Map<String, Dir> dirs = new TreeMap<>();
		final Map<String, FileEntry> files = new TreeMap<>();
	}
}
