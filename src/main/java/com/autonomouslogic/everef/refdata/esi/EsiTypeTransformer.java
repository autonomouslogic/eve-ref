package com.autonomouslogic.everef.refdata.esi;

import com.autonomouslogic.everef.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class EsiTypeTransformer implements EsiTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected EsiTypeTransformer() {}

	@Override
	public ObjectNode transformEsi(ObjectNode json, String language) throws Throwable {
		transformUtil.arrayToObject(json, "dogma_attributes", "attribute_id");
		transformUtil.arrayToObject(json, "dogma_effects", "effect_id");
		transformUtil.setPath(json, json.get("name"), "name", language);
		transformUtil.setPath(json, json.get("description"), "description", language);
		return json;
	}
}
