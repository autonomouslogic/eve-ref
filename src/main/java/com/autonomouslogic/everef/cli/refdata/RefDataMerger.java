package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.LinkedHashSet;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RefDataMerger {
	@Inject
	protected ObjectMerger objectMerger;

	@Setter
	@NonNull
	private String name;

	@Setter
	@NonNull
	private String outputStoreName;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Inject
	protected RefDataMerger() {}

	public Completable merge() {
		return Completable.fromAction(() -> {
					var sdeStore = storeHandler.getSdeStore(name);
					var esiStore = storeHandler.getEsiStore(name);
					var hoboleaksStore = storeHandler.getHoboleaksStore(name);
					var ids = new LinkedHashSet<Long>();
					ids.addAll(sdeStore.keySet());
					ids.addAll(esiStore.keySet());
					ids.addAll(hoboleaksStore.keySet());
					log.info(
							"Merging {} {} from SDE ({}) and ESI ({}) datasets",
							ids.size(),
							name,
							sdeStore.size(),
							esiStore.size());
					for (long id : ids) {
						mergeAndStore(id);
					}
				})
				.subscribeOn(Schedulers.computation());
	}

	private void mergeAndStore(long id) {
		var sdeStore = storeHandler.getSdeStore(name);
		var esiStore = storeHandler.getEsiStore(name);
		var hoboleaksStore = storeHandler.getHoboleaksStore(name);
		var refStore = storeHandler.getRefStore(outputStoreName);
		try {
			var sde = sdeStore.get(id);
			var esi = esiStore.get(id);
			var hobo = hoboleaksStore.get(id);
			var existing = refStore.get(id);
			var ref = merge(existing, sde, esi, hobo);
			refStore.put(id, ref);
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Failed merging %s [%d]", name, id), e);
		}
	}

	private JsonNode merge(JsonNode... objs) {
		JsonNode merged = null;
		for (var obj : objs) {
			if (obj != null) {
				if (merged == null) {
					merged = obj;
				} else {
					merged = objectMerger.merge(merged, obj);
				}
			}
		}
		return merged;
	}
}
