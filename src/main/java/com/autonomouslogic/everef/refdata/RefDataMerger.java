package com.autonomouslogic.everef.refdata;

import com.autonomouslogic.everef.cli.refdata.StoreSet;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.LinkedHashSet;
import java.util.Optional;
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
	private StoreSet stores;

	@Inject
	protected RefDataMerger() {}

	public Completable merge() {
		return Completable.fromAction(() -> {
					var sdeStore = stores.getSdeStore();
					var esiStore = stores.getEsiStore();
					var refStore = stores.getRefStore();
					var ids = new LinkedHashSet<Long>();
					ids.addAll(sdeStore.keySet());
					ids.addAll(esiStore.keySet());
					log.info(
							"Merging {} {} from SDE ({}) and ESI ({}) datasets",
							ids.size(),
							name,
							sdeStore.size(),
							esiStore.size());
					for (long id : ids) {
						try {
							var sde = sdeStore.get(id);
							var esi = esiStore.get(id);
							JsonNode ref;
							if (sde != null && esi != null) {
								ref = objectMerger.merge(sde, esi);
							} else {
								ref = Optional.ofNullable(sde).orElse(esi);
							}
							refStore.put(id, ref);
						} catch (Exception e) {
							throw new IllegalStateException(String.format("Failed merging %s [%d]", name, id), e);
						}
					}
				})
				.subscribeOn(Schedulers.computation());
	}
}
