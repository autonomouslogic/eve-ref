package com.autonomouslogic.everef.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class VirtualDirectoryTest {
	VirtualDirectory dir;

	@BeforeEach
	void before() {
		dir = new VirtualDirectory();
	}

	@Test
	void shouldSaveFiles() {
		dir.add(FileEntry.file("foo", 1, "abc"));
		assertTrue(dir.exists("foo"));
		assertFalse(dir.exists("foo1"));
	}

	@Test
	void shouldSaveDirectories() {
		dir.add(FileEntry.directory("foo"));
		assertTrue(dir.exists("foo"));
		assertFalse(dir.exists("foo1"));
	}

	@Test
	void shouldSaveFilesInSubdirectories() {
		dir.add(FileEntry.file("path/to/foo", 1, "abc"));
		assertTrue(dir.exists("path/to/foo"));
		assertTrue(dir.exists("path/to"));
		assertTrue(dir.exists("path"));
		assertFalse(dir.exists("path/to/foo1"));
	}

	@Test
	void shouldSaveSubDirectories() {
		dir.add(FileEntry.directory("path/to/foo"));
		assertTrue(dir.exists("path/to/foo"));
		assertTrue(dir.exists("path/to"));
		assertTrue(dir.exists("path"));
		assertFalse(dir.exists("foo"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"/path/to/foo", "path/to/foo/", "/path/to/foo/"})
	void shouldHandleSlashesInDirectoryPaths(String path) {
		dir.add(FileEntry.directory(path));
		assertTrue(dir.exists(path));
		assertTrue(dir.exists(StringUtils.removeStart(path, "/")));
		assertTrue(dir.exists(StringUtils.removeEnd(path, "/")));
		assertTrue(dir.exists(StringUtils.removeStart(StringUtils.removeEnd(path, "/"), "/")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"/path/to/foo", "path/to/foo"})
	void shouldHandleSlashesInFilePaths(String path) {
		dir.add(FileEntry.file(path));
		assertTrue(dir.exists(path));
		assertTrue(dir.exists(StringUtils.removeStart(path, "/")));
		assertTrue(dir.exists(StringUtils.removeEnd(path, "/")));
		assertTrue(dir.exists(StringUtils.removeStart(StringUtils.removeEnd(path, "/"), "/")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"path", "/path", "path/", "/path/"})
	void shouldListAllEntriesRecursively(String path) {
		dir.add(FileEntry.file("foo1"));
		dir.add(FileEntry.file("path/to/foo1"));
		dir.add(FileEntry.file("path/foo2"));
		dir.add(FileEntry.file("path/other/more/foo3"));
		dir.add(FileEntry.directory("path/abc/"));
		assertEquals(
				StringUtils.join(
						List.of(
								FileEntry.directory("path/other"),
								FileEntry.directory("path/other/more"),
								FileEntry.file("path/other/more/foo3"),
								FileEntry.directory("path/to"),
								FileEntry.file("path/to/foo1"),
								FileEntry.directory("path/abc"),
								FileEntry.file("path/foo2")),
						'\n'),
				StringUtils.join(dir.list(path, true).toList(), '\n'));
	}

	@ParameterizedTest
	@ValueSource(strings = {"path", "/path", "path/", "/path/"})
	void shouldListDirectoryEntries(String path) {
		dir.add(FileEntry.file("foo1"));
		dir.add(FileEntry.file("path/foo2"));
		dir.add(FileEntry.file("path/foo3"));
		dir.add(FileEntry.directory("path/abc"));
		dir.add(FileEntry.directory("path/abcd/"));
		dir.add(FileEntry.file("path/ab/foo4"));
		assertEquals(
				StringUtils.join(
						List.of(
								FileEntry.directory("path/ab"),
								FileEntry.directory("path/abc"),
								FileEntry.directory("path/abcd"),
								FileEntry.file("path/foo2"),
								FileEntry.file("path/foo3")),
						'\n'),
				StringUtils.join(dir.list(path, false).toList(), '\n'));
	}
}
