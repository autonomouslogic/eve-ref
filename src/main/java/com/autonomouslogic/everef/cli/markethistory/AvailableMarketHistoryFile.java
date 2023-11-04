package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.url.HttpUrl;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A market history file available on the data site.
 */
@Value
@Builder(toBuilder = true)
public class AvailableMarketHistoryFile {
	LocalDate date;
	Integer year;
	boolean ccpQuantBackfillFile;
	Pair<LocalDate, LocalDate> range;
	HttpUrl httpUrl;

	public boolean isDateFile() {
		return date != null;
	}

	public boolean isYearFile() {
		return year != null;
	}
}
