package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Region;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import org.apache.commons.io.FilenameUtils;

@Singleton
public class RefDataAccess {
	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected RefDataAccess() {}

	public <T> Flowable<T> loadReferenceDataArchive(@NonNull String type, @NonNull Class<T> model) {
		return refDataUtil.downloadLatestReferenceData().flatMapPublisher(file -> {
			return loadReferenceDataArchive(file, type, model);
		});
	}

	public <T> Flowable<T> loadReferenceDataArchive(@NonNull File file, @NonNull String type, @NonNull Class<T> model) {
		return CompressUtil.loadArchive(file).flatMap(pair -> {
			var filename = pair.getKey().getName();
			if (!filename.endsWith(".json")) {
				return Flowable.empty();
			}
			if (!type.equals(FilenameUtils.getBaseName(filename))) {
				return Flowable.empty();
			}
			var mapType = objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, model);
			Map<String, T> map = objectMapper.readValue(pair.getRight(), mapType);
			return Flowable.fromIterable(map.values());
		});
	}

	public Flowable<Region> allRegions() {
		return loadReferenceDataArchive("regions", Region.class)
			.switchIfEmpty(Flowable.error(new IllegalStateException("No regions found")));
	}

	public Flowable<InventoryType> allTypes() {
		return loadReferenceDataArchive("types", InventoryType.class)
			.switchIfEmpty(Flowable.error(new IllegalStateException("No types found")));
	}

	public Flowable<InventoryType> marketTypes() {
		return allTypes().filter(type -> type.getMarketGroupId() != null)
			.switchIfEmpty(Flowable.error(new IllegalStateException("No market types found")));
	}

	public Flowable<Region> marketRegions() {
		return allRegions()
				.filter(region -> region.getUniverseId() != null)
				.filter(region -> EveConstants.MARKET_UNIVERSE_IDS.contains(region.getUniverseId()))
			.switchIfEmpty(Flowable.error(new IllegalStateException("No market regions found")));
	}
}
