package com.autonomouslogic.everef.cli.refdata.esi;

import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class EsiDogmaAttributesTransformer implements EsiTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected EsiDogmaAttributesTransformer() {}

	@Override
	public ObjectNode transformEsi(ObjectNode json, String language) throws Throwable {
		transformUtil.setPath(json, json.get("display_name"), "display_name", language);
		transformUtil.setPath(json, json.get("description"), "description", language);
		return json;
	}
}
