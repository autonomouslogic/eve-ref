package com.autonomouslogic.everef.refdata.sde;

import com.autonomouslogic.everef.refdata.FieldRenamer;
import com.autonomouslogic.everef.refdata.SimpleStoreLoader;
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
 * Loads entries from the SDE dumps and prepares them for Ref Data.
 */
@Log4j2
public class SdeLoader {
	public static final String SDE_TYPES_PATH = "sde/fsd/typeIDs.yaml";

	@Inject
	protected FieldRenamer fieldRenamer;

	@Inject
	protected Provider<SimpleStoreLoader> simpleLoaderProvider;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> typeStore;

	@Inject
	protected SdeLoader() {}

	public Completable load(@NonNull File file) {
		return CompressUtil.loadArchive(file).flatMapCompletable(pair -> {
			switch (pair.getLeft().getName()) {
				case SDE_TYPES_PATH:
					return simpleLoaderProvider
							.get()
							.setIdFieldName("type_id")
							.setOutput(typeStore)
							.setTransformer(fieldRenamer::fieldRenameObjectFromSde)
							.readValues(pair.getRight());
				default:
					log.warn("Unknown SDE entry: {}", pair.getLeft().getName());
					break;
			}
			return Completable.complete();
		});
	}
}
