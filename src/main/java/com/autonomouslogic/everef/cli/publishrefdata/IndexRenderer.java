package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;

import javax.inject.Inject;

/**
 * Renders the root market group index.
 */
@Log4j2
public class IndexRenderer implements RefDataRenderer {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected RefDataUtil refDataUtil;

	@Setter
	@NonNull
	private MVStore dataStore;

	@Inject
	protected IndexRenderer() {}

	public Flowable<Pair<String, JsonNode>> render() {
		return Flowable.defer(() -> {
			var groups = mvStoreUtil.openJsonMap(dataStore, "market_groups", Long.class);
			return Flowable.fromIterable(groups.values())
					.map(json -> objectMapper.convertValue(json, MarketGroup.class))
					.filter(group -> group.getParentGroupId() == null)
					.map(group -> group.getMarketGroupId())
					.sorted()
					.toList()
					.map(ids -> Pair.of(
							refDataUtil.subPath("market_groups/root"), (JsonNode) objectMapper.valueToTree(ids)))
					.toFlowable();
		});
	}
}
