package com.autonomouslogic.everef.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndexFileEntry {
	private final String name;
	private final long size;
	private final Instant lastModified;
	private final String md5;
	private final String type;
	private final Instant date;

	public IndexFileEntry(String name, long size, Instant lastModified, String md5, String type, Instant date) {
		this.name = name;
		this.size = size;
		this.lastModified = lastModified;
		this.md5 = md5;
		this.type = type;
		this.date = date;
	}

	@JsonProperty
	public String getName() {
		return name;
	}

	@JsonProperty
	public long getSize() {
		return size;
	}

	@JsonProperty
	public Instant getLastModified() {
		return lastModified;
	}

	@JsonProperty
	public String getMd5() {
		return md5;
	}

	@JsonProperty
	public String getType() {
		return type;
	}

	@JsonProperty
	public Instant getDate() {
		return date;
	}

	public static IndexFileEntryBuilder builder() {
		return new IndexFileEntryBuilder();
	}

	public static class IndexFileEntryBuilder {
		private String name;
		private long size;
		private Instant lastModified;
		private String md5;
		private String type;
		private Instant date;

		public IndexFileEntryBuilder name(String name) {
			this.name = name;
			return this;
		}

		public IndexFileEntryBuilder size(long size) {
			this.size = size;
			return this;
		}

		public IndexFileEntryBuilder lastModified(Instant lastModified) {
			this.lastModified = lastModified;
			return this;
		}

		public IndexFileEntryBuilder md5(String md5) {
			this.md5 = md5;
			return this;
		}

		public IndexFileEntryBuilder type(String type) {
			this.type = type;
			return this;
		}

		public IndexFileEntryBuilder date(Instant date) {
			this.date = date;
			return this;
		}

		public IndexFileEntry build() {
			return new IndexFileEntry(name, size, lastModified, md5, type, date);
		}
	}
}
