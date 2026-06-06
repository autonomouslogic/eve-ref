package com.autonomouslogic.everef.data;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class IndexDirectoryEntry {
	@NonNull
	String name;
}
