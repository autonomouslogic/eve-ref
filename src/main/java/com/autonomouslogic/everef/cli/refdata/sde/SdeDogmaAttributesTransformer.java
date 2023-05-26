package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import javax.inject.Singleton;

@Log4j2
@Singleton
public class SdeDogmaAttributesTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected SdeDogmaAttributesTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json) throws Throwable {
		transformUtil.renameField(json, "display_name_id", "display_name");
		transformUtil.setPath(json, json.get("description"), "description", "en");
		return json;
	}
}