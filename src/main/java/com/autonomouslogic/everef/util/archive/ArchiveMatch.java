package com.autonomouslogic.everef.util.archive;

import java.time.Instant;
import java.util.Optional;
import lombok.Value;

@Value
public class ArchiveMatch {
	String type;
	Optional<Instant> date;
}
