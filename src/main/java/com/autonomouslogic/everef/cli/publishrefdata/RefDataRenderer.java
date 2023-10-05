package com.autonomouslogic.everef.cli.publishrefdata;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Flowable;
import org.apache.commons.lang3.tuple.Pair;

public interface RefDataRenderer {
	Flowable<Pair<String, JsonNode>> render();
}
