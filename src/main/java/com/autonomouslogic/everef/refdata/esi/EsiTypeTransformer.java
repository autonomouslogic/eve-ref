package com.autonomouslogic.everef.refdata.esi;

import com.autonomouslogic.everef.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.functions.BiFunction;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class EsiTypeTransformer implements BiFunction<ObjectNode, String, ObjectNode> {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected EsiTypeTransformer() {}

	@Override
	public ObjectNode apply(ObjectNode json, String language) throws Throwable {
		transformUtil.arrayToObject(json, "dogma_attributes", "attribute_id");
		transformUtil.arrayToObject(json, "dogma_effects", "effect_id");
		return json;
	}
}
