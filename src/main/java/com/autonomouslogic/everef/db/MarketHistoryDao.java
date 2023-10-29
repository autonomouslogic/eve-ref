package com.autonomouslogic.everef.db;

import com.autonomouslogic.everef.db.schema.Tables;
import com.autonomouslogic.everef.db.schema.tables.MarketHistory;
import com.autonomouslogic.everef.db.schema.tables.records.MarketHistoryRecord;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Maybe;
import java.time.LocalDate;
import java.time.ZoneOffset;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MarketHistoryDao extends BaseDao<MarketHistory, MarketHistoryRecord, MarketHistoryEntry> {
	@Inject
	protected MarketHistoryDao() {
		super(Tables.MARKET_HISTORY);
	}

	public MarketHistoryRecord toRecord(MarketHistoryEntry pojo) {
		return new MarketHistoryRecord(
				pojo.getDate(),
				pojo.getRegionId(),
				pojo.getTypeId(),
				pojo.getAverage(),
				pojo.getHighest(),
				pojo.getLowest(),
				pojo.getVolume(),
				pojo.getOrderCount(),
				pojo.getHttpLastModified().atOffset(ZoneOffset.UTC));
	}

	public MarketHistoryEntry fromRecord(MarketHistoryRecord record) {
		return MarketHistoryEntry.builder()
				.date(record.getDate())
				.regionId(record.getRegionId())
				.typeId(record.getTypeId())
				.average(record.getAverage())
				.highest(record.getHighest())
				.lowest(record.getLowest())
				.volume(record.getVolume())
				.orderCount(record.getOrderCount())
				.httpLastModified(record.getHttpLastModified().toInstant())
				.build();
	}

	public Maybe<MarketHistoryEntry> fetchByPK(LocalDate date, int regionId, int typeId) {
		return Maybe.defer(() -> {
					var record = dbAccess.context()
							.selectFrom(table)
							.where(
									Tables.MARKET_HISTORY.DATE.eq(date),
									Tables.MARKET_HISTORY.REGION_ID.eq(regionId),
									Tables.MARKET_HISTORY.TYPE_ID.eq(typeId))
							.fetchOne();
					return record == null ? Maybe.empty() : Maybe.just(fromRecord(record));
				})
				.compose(Rx.offloadMaybe());
	}
}
