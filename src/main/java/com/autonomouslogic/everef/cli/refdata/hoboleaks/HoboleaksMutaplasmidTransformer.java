package com.autonomouslogic.everef.cli.refdata.hoboleaks;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class HoboleaksMutaplasmidTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected HoboleaksMutaplasmidTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		transformMappings(json);
		transformModifications(json);
		return json;
	}

	private void transformMappings(ObjectNode json) {
		var mappings = (ArrayNode) json.get("type_mappings");
		var newMappings = json.putObject("type_mappings");
		for (var mappingNode : mappings) {
			var mappingObj = (ObjectNode) mappingNode;
			transformUtil.renameField(mappingObj, "resulting_type", "resulting_type_id");
			transformUtil.renameField(mappingObj, "applicable_types", "applicable_type_ids");
			newMappings.put(mappingObj.get("resulting_type_id").asText(), mappingObj);
		}
	}

	private void transformModifications(ObjectNode json) {
		var modifications = (ObjectNode) json.get("dogma_modifications");
		modifications.elements().forEachRemaining(entry -> {
			var obj = (ObjectNode) entry;
			if (obj.has("high_is_good")) {
				obj.put("high_is_good", obj.get("high_is_good").asBoolean());
			}
		});
	}
}
