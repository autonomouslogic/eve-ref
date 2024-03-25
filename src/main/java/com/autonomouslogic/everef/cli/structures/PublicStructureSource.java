package com.autonomouslogic.everef.cli.structures;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.IS_PUBLIC_STRUCTURE;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_SEEN_PUBLIC_ID;

import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.infrastructure.Success;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.Set;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PublicStructureSource implements StructureSource {
	@Inject
	protected UniverseApi universeApi;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected StructureScrapeHelper structureScrapeHelper;

	@Setter
	private Instant timestamp;

	@Inject
	protected PublicStructureSource() {}

	@Override
	public Flowable<Long> getStructures(@NonNull StructureStore store) {
		return Flowable.defer(() -> {
					var response = universeApi.getUniverseStructuresWithHttpInfo(
							UniverseApi.DatasourceGetUniverseStructures.tranquility, null, null);
					if (response.getStatusCode() != 200) {
						return Flowable.error(new RuntimeException(
								String.format("Failed to fetch public structure ids: %s", response.getStatusCode())));
					}
					var ids = ((Success<Set<Long>>) response).getData();
					var lastModified =
							structureScrapeHelper.getLastModified(response).orElse(timestamp);
					log.debug("Fetched {} public structure ids", ids.size());
					return Flowable.fromIterable(ids)
							.observeOn(Schedulers.computation())
							.doOnNext(id -> {
								var node = store.getOrInitStructure(id);
								node.put(IS_PUBLIC_STRUCTURE, true);
								node.put(LAST_SEEN_PUBLIC_ID, lastModified.toString());
								store.put(node);
							});
				})
				.subscribeOn(Schedulers.io());
	}
}
