package com.autonomouslogic.everef.cli.structures.source;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.IS_SOVEREIGNTY_STRUCTURE;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_SEEN_SOVEREIGNTY_STRUCTURE;

import com.autonomouslogic.everef.cli.structures.StructureScrapeHelper;
import com.autonomouslogic.everef.cli.structures.StructureStore;
import com.autonomouslogic.everef.esi.EsiConstants;
import com.autonomouslogic.everef.openapi.esi.api.SovereigntyApi;
import com.autonomouslogic.everef.util.VirtualThreads;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import javax.inject.Inject;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Deprecated
public class SovereigntyStructureSource implements StructureSource {
	@Inject
	protected SovereigntyApi sovereigntyApi;

	@Inject
	protected StructureScrapeHelper structureScrapeHelper;

	@Setter
	@Accessors(chain = false)
	private StructureStore structureStore;

	@Setter
	private Instant timestamp;

	@Inject
	protected SovereigntyStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return Flowable.defer(() -> {
			var response = VirtualThreads.offload(() -> sovereigntyApi.getSovereigntyStructuresWithHttpInfo(
					EsiConstants.Datasource.tranquility.toString(), null));
			if (response.getStatusCode() != 200) {
				return Flowable.error(new RuntimeException(
						String.format("Failed to fetch sovereignty structure ids: %s", response.getStatusCode())));
			}
			var structures = response.getData().stream().distinct().toList();
			var lastModified = structureScrapeHelper.getLastModified(response).orElse(timestamp);
			log.debug("Fetched {} sovereignty structure ids", structures.size());
			return Flowable.fromIterable(structures)
					.observeOn(VirtualThreads.SCHEDULER)
					.map(structure -> {
						var id = structure.getStructureId();
						var type = structure.getStructureTypeId();
						var system = structure.getSolarSystemId();
						var node = structureStore.getOrInitStructure(id);
						node.put(IS_SOVEREIGNTY_STRUCTURE, true);
						node.put(LAST_SEEN_SOVEREIGNTY_STRUCTURE, lastModified.toString());
						if (!node.has("type_id")) {
							node.put("type_id", type);
						}
						if (!node.has("solar_system_id")) {
							node.put("solar_system_id", system);
						}
						structureStore.put(node);
						return id;
					});
		});
	}
}
