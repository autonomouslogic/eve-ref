package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import javax.inject.Inject;

/**
 * Transforms the schematics from the SDE into a more blueprint-friendly format.
 */
public class SchematicTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected SchematicTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		transformTypes(json);
		transformUtil.renameField(json, "pins", "pin_type_ids");
		return json;
	}

	/**
	 * Takes the <code>types</code> object and converts it into <code>materials</code> and <code>products</code>,
	 * like blueprints work.
	 * @param json
	 */
	private void transformTypes(ObjectNode json) {
		var types = (ObjectNode) json.get("types");
		var materials = json.withObject("materials");
		var products = json.withObject("products");
		types.fields().forEachRemaining(entry -> {
			transformTypeEntry(entry, materials, products);
		});
		transformUtil.remove(json, "types");
	}

	private void transformTypeEntry(Map.Entry<String, JsonNode> entry, ObjectNode materials, ObjectNode products) {
		var typeId = Long.parseLong(entry.getKey());
		var json = (ObjectNode) entry.getValue();
		json.put("type_id", typeId);
		var isInput = json.get("is_input").asBoolean();
		transformUtil.remove(json, "is_input");
		var quantity = json.get("quantity").asLong();
		if (isInput) {
			materials.set(Long.toString(typeId), json);
		} else {
			products.set(Long.toString(typeId), json);
		}
	}
}
