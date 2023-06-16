package com.autonomouslogic.everef.cli.refdata.hoboleaks;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class HoboleaksMutiplasmidTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected HoboleaksMutiplasmidTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		var mappings = (ObjectNode) json.get("type_mappings");
		transformUtil.renameField(mappings, "resulting_type", "resulting_type_id");
		transformUtil.renameField(mappings, "applicable_types", "applicable_type_ids");
		return json;
	}
}
