package com.autonomouslogic.everef.cli.publishrefdata;

import io.reactivex.rxjava3.core.Flowable;
import org.apache.commons.lang3.tuple.Pair;
import tools.jackson.databind.JsonNode;

public interface RefDataRenderer {
	Flowable<Pair<String, JsonNode>> render();
}
