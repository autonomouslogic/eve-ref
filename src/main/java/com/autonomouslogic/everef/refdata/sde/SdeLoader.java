package com.autonomouslogic.everef.refdata.sde;

import com.autonomouslogic.everef.refdata.FieldRenamer;
import com.autonomouslogic.everef.refdata.SimpleStoreLoader;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.FunctionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.functions.Function;
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

	@Inject
	protected Provider<SdeTypeTransformer> sdeTypeTransformerProvider;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> typeStore;

	@Inject
	protected SdeLoader() {}

	public Completable load(@NonNull File file) {
		return CompressUtil.loadArchive(file)
				.flatMapCompletable(
						pair -> {
							SimpleStoreLoader storeLoader = null;
							Function<ObjectNode, ObjectNode> transformer = null;
							switch (pair.getLeft().getName()) {
								case SDE_TYPES_PATH:
									storeLoader = simpleLoaderProvider
											.get()
											.setIdFieldName("type_id")
											.setOutput(typeStore);
									transformer = sdeTypeTransformerProvider.get();
									break;
								default:
									log.warn(
											"Unknown SDE entry: {}",
											pair.getLeft().getName());
									return Completable.complete();
							}
							if (transformer == null) {
								storeLoader.setTransformer(fieldRenamer);
							} else {
								storeLoader.setTransformer(FunctionUtil.concat(fieldRenamer, transformer));
							}
							return storeLoader.readValues(pair.getRight());
						},
						false,
						1);
	}
}
