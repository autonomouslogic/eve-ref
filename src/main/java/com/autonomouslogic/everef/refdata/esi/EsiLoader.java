package com.autonomouslogic.everef.refdata.esi;

import com.autonomouslogic.everef.refdata.SimpleLoader;
import com.autonomouslogic.everef.util.CompressUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;

/**
 * Loads entries from the ESI dumps and prepares them for Ref Data.
 */
@Log4j2
public class EsiLoader {
	public static final String ESI_TYPES_BASE_PATH = "data/tranquility/universe/types";

	@Inject
	protected Provider<SimpleLoader> simpleLoaderProvider;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> typeStore;

	@Inject
	protected EsiLoader() {}

	public Completable load(@NonNull File file) {
		return CompressUtil.loadArchive(file).flatMapCompletable(pair -> {
			switch (pair.getLeft().getName()) {
				case ESI_TYPES_BASE_PATH + ".en-us.yaml":
					return simpleLoaderProvider
							.get()
							.setIdFieldName("type_id")
							.setOutput(typeStore)
							.readValues(pair.getRight());
				default:
					log.warn("Unknown ESI entry: {}", pair.getLeft().getName());
					break;
			}
			return Completable.complete();
		});
	}
}
