package com.autonomouslogic.everef.data;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class IndexFileEntry {
	@NonNull
	String name;

	long size;

	@NonNull
	Instant lastModified;

	String md5;

	String type;

	Instant date;
}
