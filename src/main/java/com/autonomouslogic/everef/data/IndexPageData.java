package com.autonomouslogic.everef.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndexPageData {
	private final String path;
	private final List<IndexFileEntry> files;
	private final List<IndexDirectoryEntry> directories;

	public IndexPageData(String path, List<IndexFileEntry> files, List<IndexDirectoryEntry> directories) {
		this.path = path;
		this.files = files;
		this.directories = directories;
	}

	@JsonProperty
	public String getPath() {
		return path;
	}

	@JsonProperty
	public List<IndexFileEntry> getFiles() {
		return files;
	}

	@JsonProperty
	public List<IndexDirectoryEntry> getDirectories() {
		return directories;
	}

	public static IndexPageDataBuilder builder() {
		return new IndexPageDataBuilder();
	}

	public static class IndexPageDataBuilder {
		private String path;
		private List<IndexFileEntry> files;
		private List<IndexDirectoryEntry> directories;

		public IndexPageDataBuilder path(String path) {
			this.path = path;
			return this;
		}

		public IndexPageDataBuilder files(List<IndexFileEntry> files) {
			this.files = files;
			return this;
		}

		public IndexPageDataBuilder directories(List<IndexDirectoryEntry> directories) {
			this.directories = directories;
			return this;
		}

		public IndexPageData build() {
			return new IndexPageData(path, files, directories);
		}
	}
}
