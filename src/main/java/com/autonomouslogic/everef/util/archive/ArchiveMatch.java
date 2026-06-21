package com.autonomouslogic.everef.util.archive;

import java.time.Instant;
import java.util.Optional;
import lombok.Value;

@Value
public class ArchiveMatch {
	String type;
	Optional<Instant> date;
	Optional<Long> sequence;

	public ArchiveMatch(String type, Optional<Instant> date) {
		this(type, date, Optional.empty());
	}

	public ArchiveMatch(String type, Optional<Instant> date, Optional<Long> sequence) {
		this.type = type;
		this.date = date;
		this.sequence = sequence;
	}
}
