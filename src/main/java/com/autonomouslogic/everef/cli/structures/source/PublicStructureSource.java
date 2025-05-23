package com.autonomouslogic.everef.cli.structures.source;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.IS_PUBLIC_STRUCTURE;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_SEEN_PUBLIC_STRUCTURE;

import com.autonomouslogic.everef.cli.structures.StructureScrapeHelper;
import com.autonomouslogic.everef.cli.structures.StructureStore;
import com.autonomouslogic.everef.esi.EsiConstants;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.UniverseApi;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import javax.inject.Inject;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PublicStructureSource implements StructureSource {
	@Inject
	protected UniverseApi universeApi;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected StructureScrapeHelper structureScrapeHelper;

	@Inject
	protected EsiHelper esiHelper;

	@Setter
	@Accessors(chain = false)
	private StructureStore structureStore;

	@Setter
	private Instant timestamp;

	@Inject
	protected PublicStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return Flowable.defer(() -> {
			log.info("Fetching public structure ids");
			var response = VirtualThreads.offload(() -> universeApi.getUniverseStructuresWithHttpInfo(
					EsiConstants.Datasource.tranquility.toString(), null, null));
			if (response.getStatusCode() != 200) {
				return Flowable.error(new RuntimeException(
						String.format("Failed to fetch public structure ids: %s", response.getStatusCode())));
			}
			var ids = response.getData();
			var lastModified = structureScrapeHelper.getLastModified(response).orElse(timestamp);
			log.info("Fetched {} public structure ids", ids.size());
			log.trace("Seen structure IDs: {}", ids);
			return Flowable.fromIterable(ids)
					.observeOn(VirtualThreads.SCHEDULER)
					.doOnNext(id -> {
						var node = structureStore.getOrInitStructure(id);
						node.put(IS_PUBLIC_STRUCTURE, true);
						node.put(LAST_SEEN_PUBLIC_STRUCTURE, lastModified.toString());
						structureStore.put(node);
					});
		});
	}
}
