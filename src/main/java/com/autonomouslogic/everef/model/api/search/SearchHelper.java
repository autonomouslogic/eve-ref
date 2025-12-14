package com.autonomouslogic.everef.model.api.search;

import com.autonomouslogic.everef.config.Configs;
import jakarta.inject.Inject;
import java.net.URI;
import javax.inject.Singleton;
import lombok.NonNull;

@Singleton
public class SearchHelper {
	private final URI eveRefBaseUrl = Configs.EVE_REF_BASE_URL.getRequired();
	private final URI refDataBaseUrl = Configs.REF_DATA_BASE_URL.getRequired();

	@Inject
	public SearchHelper() {}

	public URI eveRefUrl(@NonNull SearchEntryType type, long id) {
		return eveRefBaseUrl.resolve(type.getEveRefType() + "/" + id);
	}

	public URI refDataUrl(@NonNull SearchEntryType type, long id) {
		return refDataBaseUrl.resolve(type.getRefDataType() + "/" + id);
	}

	public SearchEntryUrls urls(@NonNull SearchEntryType type, long id) {
		return SearchEntryUrls.builder()
				.everef(eveRefUrl(type, id).toString())
				.referenceData(refDataUrl(type, id).toString())
				.build();
	}
}
