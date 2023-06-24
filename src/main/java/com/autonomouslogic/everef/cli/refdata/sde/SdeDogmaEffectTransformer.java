package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class SdeDogmaEffectTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected SdeDogmaEffectTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		if (json.has("modifiers")) {
			var modifiers = (ArrayNode) json.get("modifiers");
			for (JsonNode modifierNode : modifiers) {
				var modifier = (ObjectNode) modifierNode;
				if (modifier.has("operation")) {
					transformUtil.renameField(modifier, "operation", "operator");
				}
			}
		}
		return json;
	}
}
