package com.autonomouslogic.everef.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndexDirectoryEntry {
	private final String name;

	public IndexDirectoryEntry(String name) {
		this.name = name;
	}

	@JsonProperty
	public String getName() {
		return name;
	}

	public static IndexDirectoryEntryBuilder builder() {
		return new IndexDirectoryEntryBuilder();
	}

	public static class IndexDirectoryEntryBuilder {
		private String name;

		public IndexDirectoryEntryBuilder name(String name) {
			this.name = name;
			return this;
		}

		public IndexDirectoryEntry build() {
			return new IndexDirectoryEntry(name);
		}
	}
}
