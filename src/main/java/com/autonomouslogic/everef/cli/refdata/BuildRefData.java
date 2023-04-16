package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.everef.cli.Command;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BuildRefData implements Command {
	@Inject
	protected BuildRefData() {}

	@Override
	public Completable run() {
		return null;
	}
}
