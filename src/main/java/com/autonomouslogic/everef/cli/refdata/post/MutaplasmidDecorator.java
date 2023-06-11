package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.util.HoboleaksHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;

/**
 * <ul>
 *     <li>Populates mutaplasmid data for inventory types.</li>
 *     <li>Populates dynamic item data for inventory types.</li>
 *     <li>Creates mutaplasmid </li>
 * </ul>
 *
 */
@Log4j2
public class MutaplasmidDecorator {
	private static final int MUTAPLASMID_GROUP_ID = 1964;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected HoboleaksHelper hoboleaksHelper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private StoreDataHelper helper;
	private MVMap<Long, JsonNode> types;

	@Inject
	protected MutaplasmidDecorator() {}

	public Completable create() {
		return Completable.defer(() -> {
			log.info("Creating mutaplasmids");
			types = storeHandler.getRefStore("types");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			return hoboleaksHelper.fetchDynamicAttributes().flatMapCompletable(dynamicAttributes -> {
				for (var entry : dynamicAttributes.entrySet()) {
					var mutaplasmidTypeId = entry.getKey();
					var dynamics = entry.getValue();
					setIsMutaplasmid(mutaplasmidTypeId);
					for (var mapping : dynamics.getInputOutputMapping()) {
						addCreatingMutaplasmid(mapping.getResultingType(), mutaplasmidTypeId);
						setIsDynamicItem(mapping.getResultingType());
						for (var applicableType : mapping.getApplicableTypes()) {
							addApplicableMutaplasmid(applicableType, mutaplasmidTypeId);
						}
					}
				}
				return Completable.complete();
			});
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
