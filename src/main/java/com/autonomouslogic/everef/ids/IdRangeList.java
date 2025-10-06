package com.autonomouslogic.everef.ids;

import java.util.Objects;

public class IdRangeList {
	private IdRange[] ranges;

	public IdRangeList(IdRange... ranges) {
		Objects.requireNonNull(ranges);
		this.ranges = ranges;
		for (IdRange range : this.ranges) {
			Objects.requireNonNull(range);
		}
	}

	public IdRange forId(long id) {
		for (IdRange range : ranges) {
			if (id >= range.getFrom() && id <= range.getTo()) {
				return range;
			}
		}
		return null;
	}
}
