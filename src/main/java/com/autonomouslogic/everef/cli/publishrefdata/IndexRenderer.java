package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Map;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;
import org.reactivestreams.Publisher;

/**
 * Renders the index files for each type.
 */
@Log4j2
public class IndexRenderer implements RefDataRenderer {
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
	protected IndexRenderer() {}

	public Flowable<Pair<String, JsonNode>> render() {
		return Flowable.defer(() -> Flowable.fromIterable(dataStore.getMapNames())
				.flatMap(type -> renderIndex(type, mvStoreUtil.openJsonMap(dataStore, type, Long.class))));
	}

	private Publisher<Pair<String, JsonNode>> renderIndex(String type, Map<Long, JsonNode> map) {
		return Flowable.defer(() -> {
			var index = map.keySet().stream().sorted().toList();
			log.debug("Creating {} index with {} entries", type, index.size());
			return Flowable.just(Pair.of(refDataUtil.subPath(type), (JsonNode) objectMapper.valueToTree(index)));
		});
	}
}
