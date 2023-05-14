package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Log4j2
public class MarketHistoryFetcher {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	@Named("esi-market-history")
	protected OkHttpClient okHttpClient;

	@Inject
	protected MarketHistoryFetcher() {}

	public Flowable<JsonNode> fetchMarketHistory(RegionTypePair pair) {
		return Flowable.defer(() -> {
			var esiUrl = EsiUrl.builder()
					.urlPath(String.format("/markets/%s/history/?type_id=%s", pair.getRegionId(), pair.getTypeId()))
					.build();
			return esiHelper
					.fetch(esiUrl)
					.flatMapPublisher(response -> {
						int statusCode = response.code();
						if (statusCode == 404) {
							return Flowable.<ObjectNode>empty();
						} else if (statusCode == 200) {
							return esiHelper.decodeArrayNode(esiUrl, esiHelper.decodeResponse(response));
						} else {
							return Flowable.<ObjectNode>error(new RuntimeException(
									String.format("Unknown status code %s for URL %s", statusCode, esiUrl)));
						}
					})
					.doOnNext(node -> ((ObjectNode) node)
							.put("region_id", pair.getRegionId())
							.put("type_id", pair.getTypeId()))
					.retry(3, e -> {
						log.warn("Retrying {} due to {}", esiUrl, ExceptionUtils.getRootCauseMessage(e));
						return true;
					});
		});
	}
}
