package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * <ul>
 *     <li>Populates mutaplasmid data for inventory types.</li>
 *     <li>Populates dynamic item data for inventory types.</li>
 *     <li>Creates mutaplasmid entries.</li>
 * </ul>
 *
 */
@Log4j2
public class CanFitDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private StoreDataHelper helper;
	private Map<Long, JsonNode> dogmaAttributes;
	private Map<Long, JsonNode> types;

	@Inject
	protected CanFitDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating canFit");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			dogmaAttributes = storeHandler.getRefStore("dogmaAttributes");
			types = storeHandler.getRefStore("types");

			var canFitAttributes = dogmaAttributes.values().stream()
					.filter(node -> node.get("name").asText().startsWith("canFitShipType"))
					.map(node -> node.get("attribute_id").asLong())
					.toList();
			for (var canFitAttribute : canFitAttributes) {
				crossReferenceCanFitShipType(canFitAttribute);
			}
		});
	}

	private void crossReferenceCanFitShipType(long attributeId) {
		var modules = helper.getTypesWithDogmaAttribute(attributeId);
		for (var module : modules) {
			var dogma = helper.getDogmaFromType(module, attributeId).orElseThrow();
			var targetId = dogma.getValue().longValue();
			var target = Optional.ofNullable(types.get(targetId))
					.map(n -> objectMapper.convertValue(n, InventoryType.class));
			if (target.isEmpty()) {
				log.warn(
						"CanFitShipType[{}] target not found for module {} and attribute {}",
						attributeId,
						module.getTypeId(),
						attributeId);
				continue;
			}
			crossReferenceCanFitShipType(module, target.get());
		}
	}

	private void crossReferenceCanFitShipType(InventoryType module, InventoryType target) {
		var moduleId = module.getTypeId();
		var targetId = target.getTypeId();
		var moduleJson = (ObjectNode) types.get(moduleId);
		var targetJson = (ObjectNode) types.get(targetId);
		moduleJson.withArray("can_fit_types").add(targetId);
		targetJson.withArray("can_be_fitted_with_types").add(moduleId);
		types.put(moduleId, moduleJson);
		types.put(targetId, targetJson);
	}
}
