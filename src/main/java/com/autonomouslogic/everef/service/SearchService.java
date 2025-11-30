package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.model.api.search.SearchInventoryType;
import com.autonomouslogic.everef.model.api.search.SearchResult;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.util.MarketGroupHelper;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Log4j2
public class SearchService {

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected MarketGroupHelper marketGroupHelper;

	@Inject
	public SearchService() {}

	protected Pattern getSearchPattern(String q) {
		var queryParts = q.trim().split("\\s+");
		var regexString = String.join(".*", queryParts);
		return Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
	}

	public SearchResult search(String q) {
		if (q == null || q.trim().isEmpty()) {
			throw new IllegalArgumentException("Query cannot be null or empty");
		}
		if (q.length() < 3) {
			return SearchResult.builder().input(q).build();
		}

		var searchPattern = getSearchPattern(q);
		var inventoryTypeStream = refDataService
				.getLoadedRefData()
				.getAllTypes()
				.filter(Objects::nonNull)
				.filter(type -> type.getValue().getPublished());

		var searchResults = inventoryTypeStream
				.filter(type -> {
					var typeNameEn = type.getValue().getName().get("en");
					if (typeNameEn == null) {
						return false;
					}
					return searchPattern.matcher(typeNameEn).find();
				})
				.map(Pair::getValue)
				.collect(Collectors.toList());

		return SearchResult.builder()
				.input(q)
				.searchInventoryType(searchResults.stream()
						.map(item -> {
							var marketGroup = marketGroupHelper.getRootMarketGroup(item);
							return SearchInventoryType.builder()
									.typeId(item.getTypeId())
									.nameEn(item.getName().get("en"))
									.rootMarketGroup(Optional.ofNullable(marketGroup)
											.flatMap(g -> Optional.ofNullable(g.getName().get("en")))
											.orElse("Inventory type"))
									.rootMarketGroupId(Optional.ofNullable(marketGroup)
											.flatMap(g -> Optional.ofNullable(g.getMarketGroupId()))
											.orElse(null))
									.build();
						})
						.collect(Collectors.toList()))
				.build();
	}
}
