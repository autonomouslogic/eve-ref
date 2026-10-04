package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import javax.inject.Inject;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Flattens the SDE's <code>skills: [{type_id: X}, ...]</code> list into a plain <code>skill_type_ids: [X, ...]</code>
 * list.
 */
public class NpcCharacterSkillsTransformer implements SimpleTransformer {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected NpcCharacterSkillsTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		var skills = json.get("skills");
		if (skills != null && skills.isArray()) {
			var skillTypeIds = jsonMapper.createArrayNode();
			for (var skill : skills) {
				skillTypeIds.add(skill.get("type_id"));
			}
			json.set("skill_type_ids", skillTypeIds);
		}
		transformUtil.remove(json, "skills");
		return json;
	}
}
