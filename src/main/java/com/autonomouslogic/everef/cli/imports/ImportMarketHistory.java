package com.autonomouslogic.everef.cli.imports;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.markethistory.MarketHistoryLoader;
import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.db.DbAdapter;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import java.time.LocalDate;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImportMarketHistory implements Command {
	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected DbAdapter dbAdapter;

	@Inject
	protected MarketHistoryLoader marketHistoryLoader;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected ImportMarketHistory() {}

	@Override
	public Completable run() {

		return marketHistoryLoader
				.setMinDate(LocalDate.now().minusDays(14))
				.load()
				.map(pair -> objectMapper.convertValue(pair.getValue(), MarketHistoryEntry.class))
				.buffer(100)
				.flatMapCompletable(entries -> dbAdapter.insert(entries), false, 1);
	}
}
