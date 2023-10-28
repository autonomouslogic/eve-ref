package com.autonomouslogic.everef.cli.imports;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.markethistory.MarketHistoryLoader;
import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.db.schema.Tables;
import com.autonomouslogic.everef.db.schema.tables.pojos.MarketHistory;
import com.autonomouslogic.everef.db.schema.tables.records.MarketHistoryRecord;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.util.Rx;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImportMarketHistory implements Command {
	@Inject
	protected DbAccess dbAccess;

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
				.flatMapCompletable(
						entries -> Completable.fromAction(() -> {
									var stmt = dbAccess.context()
											.insertInto(Tables.MARKET_HISTORY)
											.columns(Tables.MARKET_HISTORY.fields());
									for (var entry : entries) {
										var pojo = new MarketHistory(
												entry.getDate(),
												entry.getRegionId(),
												entry.getTypeId(),
												entry.getAverage(),
												entry.getHighest(),
												entry.getLowest(),
												entry.getVolume(),
												entry.getOrderCount(),
												entry.getHttpLastModified().atOffset(ZoneOffset.UTC));
										var record = new MarketHistoryRecord(pojo);
										stmt = stmt.values(record);
									}
									stmt.onDuplicateKeyIgnore().execute();
								})
								.compose(Rx.offloadCompletable()),
						false,
						1);
	}
}
