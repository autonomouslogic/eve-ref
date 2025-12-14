package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.api.search.SearchEntry;
import com.autonomouslogic.everef.model.api.search.SearchEntryType;
import com.autonomouslogic.everef.model.api.search.SearchHelper;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.service.RefDataService;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import javax.inject.Singleton;

@Singleton
public class GroupSearchEntryFactory implements SearchEntryFactory {
	public static final String DEFAULT_TYPE_NAME = "Inventory group";

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected SearchHelper searchHelper;

	@Inject
	protected GroupSearchEntryFactory() {}

	@Override
	public Stream<SearchEntry> createEntries() {
		var loadedRefData = refDataService.getLoadedRefData();
		return loadedRefData.getAllGroups().flatMap(pair -> inventoryGroup(pair.getValue(), loadedRefData));
	}

	private Stream<SearchEntry> inventoryGroup(InventoryGroup group, LoadedRefData loadedRefData) {
		var title = group.getName().get("en");
		if (title == null) {
			return Stream.empty();
		}

		return Stream.of(SearchEntry.builder()
				.title(title)
				.language("en")
				.id(group.getGroupId())
				.type(SearchEntryType.GROUP)
				.typeName(DEFAULT_TYPE_NAME)
				.urls(searchHelper.urls(SearchEntryType.GROUP, group.getGroupId()))
				.build());
	}
}
