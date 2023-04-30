package com.autonomouslogic.everef.cli.refdata.esi;

import com.autonomouslogic.everef.cli.refdata.SimpleStoreLoader;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
	private EsiTransformer esiTransformer;

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
			transformed = esiTransformer.transformEsi(transformed, language);
		}
		return super.readValue(id, transformed);
	}
}
