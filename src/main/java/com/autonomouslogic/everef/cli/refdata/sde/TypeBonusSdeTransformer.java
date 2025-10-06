package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;

public class TypeBonusSdeTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected TypeBonusSdeTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode traits, String language) throws Throwable {
		var json = objectMapper.createObjectNode();
		json.set("traits", traits);
		transformUtil.arrayToObject(traits, "misc_bonuses", "importance");
		transformUtil.arrayToObject(traits, "role_bonuses", "importance");
		if (traits.has("types")) {
			var types = (ObjectNode) traits.get("types");
			types.fields().forEachRemaining(pair -> {
				transformUtil.arrayToObject(types, pair.getKey(), "importance");
			});
		}
		return json;
	}
}
