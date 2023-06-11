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
					setIsMutaplasmid(mutaplasmidTypeId);
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
}
