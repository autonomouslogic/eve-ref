package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;

/**
 * Renders the basic objects in the reference data collections.
 */
@Log4j2
public class BasicFileRenderer implements RefDataRenderer {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected RefDataUtil refDataUtil;

	@Setter
	@NonNull
	private MVStore dataStore;

	@Inject
	protected BasicFileRenderer() {}

	public Flowable<Pair<String, JsonNode>> render() {
		return Flowable.fromIterable(dataStore.getMapNames()).flatMap(mapName -> {
			log.info("Rendering {}", mapName);
			var map = mvStoreUtil.openJsonMap(dataStore, mapName, Long.class);
			return Flowable.fromIterable(map.entrySet()).map(entry -> {
				var id = entry.getKey();
				var node = entry.getValue();
				return Pair.of(refDataUtil.subPath(mapName, id), node);
			});
		});
	}
}
