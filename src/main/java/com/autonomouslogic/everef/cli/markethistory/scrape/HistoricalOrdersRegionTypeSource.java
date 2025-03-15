package com.autonomouslogic.everef.cli.markethistory.scrape;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.io.File;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * Provides region-type pairs based on historical market order snapshots.
 */
@Log4j2
class HistoricalOrdersRegionTypeSource implements RegionTypeSource {
	@Inject
	protected Provider<DataCrawler> dataCrawler;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected CsvMapper csvMapper;

	private Period maxAge = Configs.ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK.getRequired();

	@Setter
	@NonNull
	private LocalDate today;

	@Inject
	protected HistoricalOrdersRegionTypeSource() {}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		return getSampleFiles()
				.flatMap(f -> downloadFile(f).toFlowable())
				.flatMap(this::parseFile)
				.onErrorResumeNext(e -> {
					log.warn("Failed fetching historical region orders, ignoring", e);
					return Flowable.empty();
				});
	}

	@NotNull
	private Flowable<DataUrl> getSampleFiles() {
		var minTime = today.atStartOfDay().atZone(ZoneOffset.UTC).minus(maxAge).toInstant();
		return dataCrawler
				.get()
				.setPrefix(ArchivePathFactory.MARKET_ORDERS.getFolder())
				.crawl()
				.flatMap(url -> {
					var time = ArchivePathFactory.MARKET_ORDERS.parseArchiveTime(url.getPath());
					if (time == null || time.isBefore(minTime)) {
						return Flowable.empty();
					}
					return Flowable.just(Pair.of(time, url));
				})
				.groupBy(pair -> pair.getLeft().atZone(ZoneOffset.UTC).toLocalDate(), Pair::getRight)
				.flatMap(group -> group.sorted(Ordering.natural().onResultOf(DataUrl::getPath))
						.toList()
						.flatMapPublisher(list -> {
							Collections.shuffle(list);
							var selected = list.get(0);
							log.debug(
									"Saw {} files for {}, selected {}",
									list.size(),
									group.getKey(),
									selected.getPath());
							return Flowable.just(selected);
						}));
	}

	private Maybe<File> downloadFile(DataUrl url) {
		return Maybe.defer(() -> {
			var file = tempFiles
					.tempFile("market-orders", ArchivePathFactory.MARKET_ORDERS.getSuffix())
					.toFile();
			return okHttpHelper.download(url.toString(), file, okHttpClient).flatMapMaybe(response -> {
				if (response.code() != 200) {
					return Maybe.error(
							new RuntimeException("Failed to download " + url + " with code " + response.code()));
				}
				return Maybe.just(file);
			});
		});
	}

	private Flowable<RegionTypePair> parseFile(File file) {
		return Flowable.fromIterable(VirtualThreads.offload(() -> {
			log.trace("Reading market order file {}", file);
			var in = CompressUtil.uncompress(file);
			var schema = csvMapper
					.schemaFor(Object.class)
					.withHeader()
					.withStrictHeaders(true)
					.withColumnReordering(true);
			MappingIterator<JsonNode> iterator =
					csvMapper.readerFor(JsonNode.class).with(schema).readValues(in);
			var pairs = new HashSet<RegionTypePair>();
			iterator.forEachRemaining(node -> {
				pairs.add(RegionTypePair.fromHistory(node));
			});
			return pairs;
		}));
	}
}
