package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.LinkedHashSet;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;

@Log4j2
public class RefDataMerger {
	@Inject
	protected ObjectMerger objectMerger;

	@Setter
	@NonNull
	private String name;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> sdeStore;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> esiStore;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> refStore;

	@Inject
	protected RefDataMerger() {}

	public Completable merge() {
		return Completable.fromAction(() -> {
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
					var ref = objectMerger.merge(sde, esi);
					refStore.put(id, ref);
				} catch (Exception e) {
					throw new IllegalStateException(String.format("Failed merging %s [%d]", name, id), e);
				}
			}
		});
	}
}
