package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.api.search.SearchEntry;
import com.autonomouslogic.everef.model.api.search.SearchEntryType;
import com.autonomouslogic.everef.model.api.search.SearchHelper;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.util.MarketGroupHelper;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Singleton;

@Singleton
public class InventoryTypeSearchEntryFactory implements SearchEntryFactory {
	public static final String DEFAULT_TYPE_NAME = "Inventory type";

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected MarketGroupHelper marketGroupHelper;

	@Inject
	protected SearchHelper searchHelper;

	@Inject
	protected InventoryTypeSearchEntryFactory() {}

	@Override
	public Stream<SearchEntry> createEntries() {
		var loadedRefData = refDataService.getLoadedRefData();
		return loadedRefData.getAllTypes().flatMap(pair -> inventoryType(pair.getValue(), loadedRefData));
	}

	private Stream<SearchEntry> inventoryType(InventoryType type, LoadedRefData loadedRefData) {
		var title = type.getName().entrySet().stream()
				.filter(entry ->
						entry.getKey().equals("en")) // only English for now, otherwise the search file becomes massive
				.map(entry -> entry.getValue())
				.findFirst();
		if (title.isEmpty()) {
			return Stream.empty();
		}

		String typeName;
		if (type.getMarketGroupId() != null) {
			typeName = Optional.ofNullable(marketGroupHelper.getRootMarketGroup(type))
					.flatMap(g -> Optional.ofNullable(g.getName().get("en")))
					.orElse(DEFAULT_TYPE_NAME);
		} else if (type.getGroupId() != null) {
			typeName = Optional.ofNullable(marketGroupHelper.getRootMarketGroup(type))
					.flatMap(g -> {
						return Optional.ofNullable(g.getName()).flatMap(m -> Optional.ofNullable(m.get("en")));
					})
					.orElse(DEFAULT_TYPE_NAME);
		} else {
			return Stream.empty();
		}

		return Stream.of(SearchEntry.builder()
				.title(title.get())
				.language("en")
				.id(type.getTypeId())
				.type(SearchEntryType.INVENTORY_TYPE)
				.typeName(typeName)
				.urls(searchHelper.urls(SearchEntryType.INVENTORY_TYPE, type.getTypeId()))
				.build());
	}
}
