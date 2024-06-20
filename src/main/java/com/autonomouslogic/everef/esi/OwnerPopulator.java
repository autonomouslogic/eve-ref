package com.autonomouslogic.everef.esi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * Populates owner information for structures.
 */
@Singleton
@Log4j2
public class OwnerPopulator {

	@Inject
	protected CharacterEsi characterEsi;

	@Inject
	protected CorporationEsi corporationEsi;

	@Inject
	protected AllianceEsi allianceEsi;

	@Inject
	protected OwnerPopulator() {}

	public Maybe<ObjectNode> populateOwner(ObjectNode structure) {
		return Maybe.defer(() -> {

		});
	}




	private Completable populateOwners() {
		return Completable.defer(() -> {
			log.info("Populating owners");
			return structureStore.allStructures().flatMapCompletable(pair -> {
				var structure = pair.getValue();
				var ownerId = structure.get(OWNER_ID);
				if (ownerId == null || ownerId.isNull()) {
					return Completable.complete();
				}
				return corporationEsi.getCorporation(ownerId.intValue()).flatMapCompletable(corporation -> {
					structure.put(OWNER_NAME, corporation.getName());
					Optional.ofNullable(corporation.getAllianceId())
						.ifPresent(allianceId -> structure.put(ALLIANCE_ID, allianceId));
					structureStore.put(structure);
					return Completable.complete();
				});
			});
		});
	}

	private Single<Boolean> populateCorporation() {
		return Single.just(false);
	}

	private Completable populateAlliances() {
		return Completable.defer(() -> {
			log.info("Populating alliances");
			return structureStore.allStructures().flatMapCompletable(pair -> {
				var structure = pair.getValue();
				var allianceId = structure.get(ALLIANCE_ID);
				if (allianceId == null || allianceId.isNull()) {
					return Completable.complete();
				}
				return allianceEsi.getAlliance(allianceId.intValue()).flatMapCompletable(alliance -> {
					structure.put(ALLIANCE_NAME, alliance.getName());
					structureStore.put(structure);
					return Completable.complete();
				});
			});
		});
	}
}
