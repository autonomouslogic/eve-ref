package com.autonomouslogic.everef.db;

import com.autonomouslogic.everef.db.schema.Tables;
import com.autonomouslogic.everef.db.schema.tables.MarketHistory;
import com.autonomouslogic.everef.db.schema.tables.records.MarketHistoryRecord;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.util.Rx;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.jooq.impl.DSL;

@Singleton
@Log4j2
public class MarketHistoryDao extends BaseDao<MarketHistory, MarketHistoryRecord, MarketHistoryEntry> {
	private final Cache<LocalDate, Map<LocalDate, Integer>> dailyPairsCache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(1))
			.initialCapacity(1)
			.softValues()
			.build();

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
				pojo.getHttpLastModified().atOffset(ZoneOffset.UTC)
				//			Optional.ofNullable(pojo.getHttpLastModified())
				//					.map(t -> t.atOffset(ZoneOffset.UTC))
				//						.orElse(null)
				);
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

	/**
	 * Fetches the daily pairs from the database.
	 * @param minDate
	 * @return
	 */
	public Single<Map<LocalDate, Integer>> fetchDailyPairs(LocalDate minDate) {
		return Single.defer(() -> {
			var cached = dailyPairsCache.getIfPresent(minDate);
			if (cached != null) {
				return Single.just(cached);
			}
			return Single.fromCallable(() -> {
						var stmt = dbAccess.context()
								.select(
										Tables.MARKET_HISTORY.DATE,
										DSL.countDistinct(
														Tables.MARKET_HISTORY.REGION_ID, Tables.MARKET_HISTORY.TYPE_ID)
												.as("pairs"))
								.from(Tables.MARKET_HISTORY)
								.where(Tables.MARKET_HISTORY.DATE.greaterOrEqual(minDate))
								.groupBy(Tables.MARKET_HISTORY.DATE);
						var dailyPairs = new HashMap<LocalDate, Integer>();
						stmt.fetch().forEach(r -> dailyPairs.put(r.value1(), r.value2()));
						return dailyPairs;
					})
					.compose(Rx.offloadSingle())
					.doOnSuccess(dailyPairs -> dailyPairsCache.put(minDate, dailyPairs));
		});
	}
}
