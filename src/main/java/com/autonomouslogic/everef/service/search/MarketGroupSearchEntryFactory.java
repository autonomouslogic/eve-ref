package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.api.search.SearchEntry;
import com.autonomouslogic.everef.model.api.search.SearchEntryType;
import com.autonomouslogic.everef.model.api.search.SearchHelper;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.service.RefDataService;
import jakarta.inject.Inject;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;

@Singleton
public class MarketGroupSearchEntryFactory implements SearchEntryFactory {
	@Inject
	protected RefDataService refDataService;

	@Inject
	protected SearchHelper searchHelper;

	@Inject
	protected MarketGroupSearchEntryFactory() {}

	@Override
	public Stream<SearchEntry> createEntries() {
		var loadedRefData = refDataService.getLoadedRefData();
		return loadedRefData.getAllMarketGroups().flatMap(pair -> marketGroup(pair.getValue(), loadedRefData));
	}

	private Stream<SearchEntry> marketGroup(MarketGroup group, LoadedRefData loadedRefData) {
		var name = group.getName().get("en");
		if (name == null) {
			return Stream.empty();
		}

		var title = marketGroupChain(group, loadedRefData).collect(Collectors.joining(" > "));

		return Stream.of(SearchEntry.builder()
				.title(title)
				.query(name)
				.language("en")
				.id(group.getMarketGroupId())
				.type(SearchEntryType.MARKET_GROUP)
				.typeName("Market group")
				.urls(searchHelper.urls(SearchEntryType.MARKET_GROUP, group.getMarketGroupId()))
				.build());
	}

	private Stream<String> marketGroupChain(MarketGroup group, LoadedRefData loadedRefData) {
		var name = group.getName().get("en");
		if (group.getParentGroupId() == null) {
			return Stream.of(name);
		}
		var parentGroup = loadedRefData.getMarketGroup(group.getParentGroupId());
		if (parentGroup == null) {
			return Stream.of(name);
		}
		return Stream.concat(marketGroupChain(parentGroup, loadedRefData), Stream.of(name));
	}
}
