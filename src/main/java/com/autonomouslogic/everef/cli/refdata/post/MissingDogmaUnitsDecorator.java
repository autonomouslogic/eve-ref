package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;

/**
 * There are some dogma attribute which do not have a unit referenced on them.
 * This decorator fixes those units.
 */
@Log4j2
public class MissingDogmaUnitsDecorator implements PostDecorator {
	private static final Map<Integer, Integer> dogmaUnits = Map.of(
			// Radius -> meters
			162, 1);

	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private StoreDataHelper helper;
	private MVMap<Long, JsonNode> dogmaAttributes;

	@Inject
	protected MissingDogmaUnitsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating blueprints");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			dogmaAttributes = storeHandler.getRefStore("dogmaAttributes");
			dogmaUnits.forEach(this::populateUnit);
		});
	}

	private void populateUnit(long attributeId, Integer unitId) {
		var json = (ObjectNode) dogmaAttributes.get(attributeId);
		Objects.requireNonNull(json, "Missing dogma attribute: " + attributeId);
		var attr = objectMapper.convertValue(json, DogmaAttribute.class);
		if (attr.getUnitId() != null) {
			if (attr.getUnitId().equals(unitId)) {
				log.debug(
						"Redundant attempt to add missing unit on dogma attribute [{}], already present", attributeId);
				return;
			}
			log.warn(
					"Attempting to add missing unit [{}] to dogma attribute [{}], but unit is already present: [{}]",
					unitId,
					attributeId,
					attr.getUnitId());
		}
		json.put("unit_id", unitId);
		dogmaAttributes.put(attributeId, json);
	}
}
