package com.autonomouslogic.everef.refdata.esi;

import com.autonomouslogic.everef.refdata.SimpleStoreLoader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.functions.BiFunction;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Reads big objects from a YAML file, transforms and converts them and stores them in the target map.
 */
@Log4j2
public class EsiStoreLoader extends SimpleStoreLoader {
	@Setter
	@NonNull
	private BiFunction<ObjectNode, String, ObjectNode> esiTransformer;

	@Setter
	@NonNull
	private String language;

	@Inject
	protected EsiStoreLoader() {}

	@Override
	@SneakyThrows
	protected ObjectNode readValue(long id, ObjectNode json) {
		var transformed = json;
		if (esiTransformer != null) {
			transformed = esiTransformer.apply(transformed, language);
		}
		return super.readValue(id, transformed);
	}
}
