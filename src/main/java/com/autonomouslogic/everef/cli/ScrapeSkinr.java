package com.autonomouslogic.everef.cli;

import java.time.ZonedDateTime;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ScrapeSkinr implements Command {
	@Setter
	private ZonedDateTime scrapeTime;

	@Inject
	protected ScrapeSkinr() {}

	@Override
	public void run() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
