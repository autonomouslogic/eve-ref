package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.api.search.SearchEntry;
import com.autonomouslogic.everef.model.api.search.SearchEntryType;
import com.autonomouslogic.everef.model.api.search.SearchHelper;
import com.autonomouslogic.everef.refdata.InventoryCategory;
import com.autonomouslogic.everef.service.RefDataService;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import javax.inject.Singleton;

@Singleton
public class CategorySearchEntryFactory implements SearchEntryFactory {
	public static final String DEFAULT_TYPE_NAME = "Inventory category";

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected SearchHelper searchHelper;

	@Inject
	protected CategorySearchEntryFactory() {}

	@Override
	public Stream<SearchEntry> createEntries() {
		var loadedRefData = refDataService.getLoadedRefData();
		return loadedRefData.getAllCategories().flatMap(pair -> inventoryCategory(pair.getValue(), loadedRefData));
	}

	private Stream<SearchEntry> inventoryCategory(InventoryCategory category, LoadedRefData loadedRefData) {
		var title = category.getName().get("en");
		if (title == null) {
			return Stream.empty();
		}

		return Stream.of(SearchEntry.builder()
				.title(title)
				.language("en")
				.id(category.getCategoryId())
				.type(SearchEntryType.CATEGORY)
				.typeName(DEFAULT_TYPE_NAME)
				.urls(searchHelper.urls(SearchEntryType.CATEGORY, category.getCategoryId()))
				.build());
	}
}
