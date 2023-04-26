package com.autonomouslogic.everef.refdata.sde;

import com.autonomouslogic.everef.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.functions.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class SdeTypeTransformer implements Function<ObjectNode, ObjectNode> {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected SdeTypeTransformer() {}

	@Override
	public ObjectNode apply(ObjectNode json) throws Throwable {
		if (json.has("traits")) {
			var traits = (ObjectNode) json.get("traits");
			transformUtil.arrayToObject(traits, "misc_bonuses", "importance");
			transformUtil.arrayToObject(traits, "role_bonuses", "importance");
			if (traits.has("types")) {
				var types = (ObjectNode) traits.get("types");
				types.fields().forEachRemaining(pair -> {
					transformUtil.arrayToObject(types, pair.getKey(), "importance");
				});
			}
		}
		return json;
	}
}