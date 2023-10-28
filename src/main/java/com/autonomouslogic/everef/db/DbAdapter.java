package com.autonomouslogic.everef.db;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.db.schema.Tables;
import com.autonomouslogic.everef.db.schema.tables.records.MarketHistoryRecord;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.time.ZoneOffset;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DbAdapter {
	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected DbAdapter() {}

	public String getTableName(Class<?> clazz) {
		var prefix = Configs.DATABASE_TABLE_NAME_PREFIX.getRequired();
		if (clazz.equals(MarketHistoryEntry.class)) {
			return prefix + "market_history";
		}
		throw new IllegalArgumentException("Unknown class: " + clazz.getName());
	}

	public Completable insert(MarketHistoryEntry entry) {
		return insert(List.of(entry));
	}

	public Completable insert(List<MarketHistoryEntry> entries) {
		return Completable.fromAction(() -> {
					var stmt = dbAccess.context()
							.insertInto(Tables.MARKET_HISTORY)
							.columns(Tables.MARKET_HISTORY.fields());
					for (var entry : entries) {
						var record = new MarketHistoryRecord(
								entry.getDate(),
								entry.getRegionId(),
								entry.getTypeId(),
								entry.getAverage(),
								entry.getHighest(),
								entry.getLowest(),
								entry.getVolume(),
								entry.getOrderCount(),
								entry.getHttpLastModified().atOffset(ZoneOffset.UTC));
						stmt = stmt.values(record);
					}
					stmt.onDuplicateKeyIgnore().execute();
				})
				.compose(Rx.offloadCompletable());
	}

	public FlowableTransformer<MarketHistoryEntry, MarketHistoryEntry> insert() {}
}
