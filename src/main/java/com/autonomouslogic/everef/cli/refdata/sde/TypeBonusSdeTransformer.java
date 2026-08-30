package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import javax.inject.Inject;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public class TypeBonusSdeTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected TypeBonusSdeTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode traits, String language) throws Throwable {
		var json = jsonMapper.createObjectNode();
		json.set("traits", traits);
		transformUtil.arrayToObject(traits, "misc_bonuses", "importance");
		transformUtil.arrayToObject(traits, "role_bonuses", "importance");
		if (traits.has("types")) {
			var types = (ObjectNode) traits.get("types");
			types.properties().forEach(pair -> {
				transformUtil.arrayToObject(types, pair.getKey(), "importance");
			});
		}
		return json;
	}
}
