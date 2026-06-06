package com.autonomouslogic.everef.data;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class IndexPageData {
	@NonNull
	String path;

	@NonNull
	List<IndexFileEntry> files;

	@NonNull
	List<IndexDirectoryEntry> directories;
}

