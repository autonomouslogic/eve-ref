package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.inject.Inject;

public class MasteriesSdeTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected MasteriesSdeTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode masteries, String language) throws Throwable {
		var json = objectMapper.createObjectNode();
		json.set("masteries", masteries);
		return json;
	}
}
