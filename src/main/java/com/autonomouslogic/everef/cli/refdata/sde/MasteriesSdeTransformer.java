package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import javax.inject.Inject;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public class MasteriesSdeTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected MasteriesSdeTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode masteries, String language) throws Throwable {
		var json = jsonMapper.createObjectNode();
		json.set("masteries", masteries);
		return json;
	}
}
