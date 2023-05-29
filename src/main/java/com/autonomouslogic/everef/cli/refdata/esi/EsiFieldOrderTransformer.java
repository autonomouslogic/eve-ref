package com.autonomouslogic.everef.cli.refdata.esi;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orders the <code>name</code> and <code>description</code> fields in the JSON.
 */
@Singleton
public class EsiFieldOrderTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected EsiFieldOrderTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		orderNodes(json, "name");
		orderNodes(json, "description");
		return json;
	}

	private void orderNodes(ObjectNode json, String field) {
		var ordered = Optional.ofNullable(json.get(field))
				.filter(node -> node.isObject())
				.map(n -> transformUtil.orderKeys((ObjectNode) n));
		if (ordered.isPresent()) {
			json.set(field, ordered.get());
		}
	}
}
