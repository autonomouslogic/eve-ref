package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.refdata.InventoryType;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
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
	public SearchService() {}

	protected Pattern getSearchPatter(String q) {
		var queryParts = q.trim().split("\\s+");
		var regexString = String.join(".*", queryParts);
		return Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
	}

	public List<InventoryType> searchType(String q) {
		if (q == null || q.trim().isEmpty()) {
			return List.of();
		}
		var searchPatter = getSearchPatter(q);
		var inventoryTypeStream = refDataService
				.getLoadedRefData()
				.getAllTypes()
				.filter(Objects::nonNull)
				.filter(type -> type.getValue().getPublished());

		return inventoryTypeStream
				.filter(type -> {
					var typeNameEn = type.getValue().getName().get("en");
					if (typeNameEn == null) {
						return false;
					}
					return searchPatter.matcher(typeNameEn).find();
				})
				.map(Pair::getValue)
				.collect(Collectors.toList());
	}
}
