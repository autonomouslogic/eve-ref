package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class SdeDogmaAttributesTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected SdeDogmaAttributesTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		transformUtil.renameField(json, "display_name_id", "display_name");
		transformUtil.renameField(json, "tooltip_title_id", "tooltip_title");
		transformUtil.renameField(json, "tooltip_description_id", "tooltip_description");
		transformUtil.setPath(json, json.get("description"), "description", "en");
		return json;
	}
}
