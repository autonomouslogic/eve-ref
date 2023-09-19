package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.Mutaplasmid;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * <ul>
 *     <li>Populates mutaplasmid data for inventory types.</li>
 *     <li>Populates dynamic item data for inventory types.</li>
 *     <li>Creates mutaplasmid </li>
 * </ul>
 *
 */
@Log4j2
public class MutaplasmidDecorator implements PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private StoreDataHelper helper;
	private Map<Long, JsonNode> types;
	private Map<Long, JsonNode> mutaplasmids;

	@Inject
	protected MutaplasmidDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating mutaplasmids");
			types = storeHandler.getRefStore("types");
			mutaplasmids = storeHandler.getRefStore("mutaplasmids");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			for (Map.Entry<Long, JsonNode> entry : mutaplasmids.entrySet()) {
				var mutaplasmid = objectMapper.convertValue(entry.getValue(), Mutaplasmid.class);
				setIsMutaplasmid(mutaplasmid.getTypeId());
				for (var mapping : mutaplasmid.getTypeMappings().values()) {
					addCreatingMutaplasmid(mapping.getResultingTypeId(), mutaplasmid.getTypeId());
					setIsDynamicItem(mapping.getResultingTypeId());
					for (var applicableType : mapping.getApplicableTypeIds()) {
						addApplicableMutaplasmid(applicableType, mutaplasmid.getTypeId());
					}
				}
			}
		});
	}

	private void setIsMutaplasmid(long typeId) {
		var type = (ObjectNode) types.get(typeId);
		if (type == null) {
			log.warn("Could not set type {} as mutaplasmid, not found", typeId);
			return;
		}
		type.put("is_mutaplasmid", true);
		types.put(typeId, type);
	}

	private void addApplicableMutaplasmid(long typeId, long mutaplasmidTypeId) {
		var type = (ObjectNode) types.get(typeId);
		if (type == null) {
			log.warn("Could not set type {} as being mutable by mutaplasmid, not found", typeId);
			return;
		}
		var array = type.withArray("applicable_mutaplasmid_type_ids");
		array.add(mutaplasmidTypeId);
		types.put(typeId, type);
	}

	private void addCreatingMutaplasmid(long typeId, long mutaplasmidTypeId) {
		var type = (ObjectNode) types.get(typeId);
		if (type == null) {
			log.warn("Could not set type {} as being created by mutaplasmid, not found", typeId);
			return;
		}
		var array = type.withArray("creating_mutaplasmid_type_ids");
		array.add(mutaplasmidTypeId);
		types.put(typeId, type);
	}

	private void setIsDynamicItem(long typeId) {
		var type = (ObjectNode) types.get(typeId);
		if (type == null) {
			log.warn("Could not set type {} as dynamic item, not found", typeId);
			return;
		}
		type.put("is_dynamic_item", true);
		types.put(typeId, type);
	}
}
