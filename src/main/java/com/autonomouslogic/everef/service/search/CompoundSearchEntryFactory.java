package com.autonomouslogic.everef.service.search;

import com.autonomouslogic.everef.model.api.search.SearchEntry;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import javax.inject.Singleton;

@Singleton
public class CompoundSearchEntryFactory implements SearchEntryFactory {
	@Inject
	protected InventoryTypeSearchEntryFactory inventoryTypes;

	@Inject
	protected MarketGroupSearchEntryFactory marketGroups;

	@Inject
	protected CategorySearchEntryFactory categories;

	@Inject
	protected GroupSearchEntryFactory groups;

	@Inject
	protected CompoundSearchEntryFactory() {}

	@Override
	public Stream<SearchEntry> createEntries() {
		return Stream.of(
						inventoryTypes.createEntries(),
						marketGroups.createEntries(),
						categories.createEntries(),
						groups.createEntries())
				.flatMap(stream -> stream);
	}
}
