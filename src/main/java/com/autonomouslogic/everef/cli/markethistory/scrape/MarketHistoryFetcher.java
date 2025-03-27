package com.autonomouslogic.everef.cli.markethistory.scrape;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Log4j2
class MarketHistoryFetcher {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	@Named("esi-market-history")
	protected OkHttpClient okHttpClient;

	@Setter
	@NonNull
	private MarketHistorySourceStats stats;

	@Inject
	protected MarketHistoryFetcher() {}

	public Flowable<JsonNode> fetchMarketHistory(RegionTypePair pair) {
		return Flowable.defer(() -> {
			var esiUrl = EsiUrl.builder()
					.urlPath(String.format("/markets/%s/history/?type_id=%s", pair.getRegionId(), pair.getTypeId()))
					.build();
			return esiHelper
					.fetch(esiUrl)
					.toFlowable()
					.compose(esiHelper.standardErrorHandling(esiUrl))
					.flatMap(response -> {
						int statusCode = response.code();
						if (statusCode == 200) {
							return esiHelper
									.decodeArrayNode(esiUrl, esiHelper.decodeResponse(response))
									.map(e -> esiHelper.populateLastModified(e, response))
									.doOnNext(e -> stats.hit(pair));
						} else {
							log.warn("Unknown status code {} for URL {}", statusCode, esiUrl);
							return Flowable.empty();
						}
					})
					.doOnNext(node -> ((ObjectNode) node)
							.put("region_id", pair.getRegionId())
							.put("type_id", pair.getTypeId()))
					.compose(Rx3Util.retryWithDelayFlowable(2, Duration.ofSeconds(10), e -> {
						log.warn("Retrying {} due to {}", esiUrl, ExceptionUtils.getRootCauseMessage(e));
						return true;
					}));
		});
	}
}
