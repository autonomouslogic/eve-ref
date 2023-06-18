package com.autonomouslogic.everef.cli.refdata.transformer;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;

public class BlueprintTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected BlueprintTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		var activities = (ObjectNode) json.get("activities");
		activities.fields().forEachRemaining(entry -> transformBlueprintActivity((ObjectNode) entry.getValue()));
		return json;
	}

	private void transformBlueprintActivity(ObjectNode activity) {
		if (activity.has("skills")) {
			activity.set("required_skills", transformRequiredSkills((ArrayNode) activity.get("skills")));
			activity.remove("skills");
		}
		if (activity.has("materials")) {
			transformUtil.arrayToObject(activity, "materials", "type_id");
		}
		if (activity.has("products")) {
			transformUtil.arrayToObject(activity, "products", "type_id");
		}
	}

	private ObjectNode transformRequiredSkills(ArrayNode skills) {
		var requiredSkills = objectMapper.createObjectNode();
		for (JsonNode skill : skills) {
			requiredSkills.put(skill.get("type_id").asText(), skill.get("level").asInt());
		}
		return requiredSkills;
	}
}
