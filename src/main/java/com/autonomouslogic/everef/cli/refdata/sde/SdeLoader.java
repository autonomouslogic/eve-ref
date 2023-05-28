package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.FieldRenamer;
import com.autonomouslogic.everef.cli.refdata.SimpleStoreLoader;
import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.autonomouslogic.everef.util.CompressUtil;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Loads entries from the SDE dumps and prepares them for Ref Data.
 */
@Log4j2
public class SdeLoader {
	public static final String SDE_TYPES_PATH = "sde/fsd/typeIDs.yaml";
	public static final String SDE_DOGMA_ATTRIBUTES_PATH = "sde/fsd/dogmaAttributes.yaml";

	@Inject
	protected FieldRenamer fieldRenamer;

	@Inject
	protected Provider<SimpleStoreLoader> simpleLoaderProvider;

	@Inject
	protected Provider<SdeTypeTransformer> sdeTypeTransformerProvider;

	@Inject
	protected Provider<SdeDogmaAttributesTransformer> sdeDogmaAttributesTransformerProvider;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Inject
	protected SdeLoader() {}

	public Completable load(@NonNull File file) {
		return CompressUtil.loadArchive(file)
				.flatMapCompletable(
						pair -> {
							SimpleStoreLoader storeLoader = null;
							SimpleTransformer transformer = null;
							switch (pair.getLeft().getName()) {
								case SDE_TYPES_PATH:
									storeLoader = simpleLoaderProvider
											.get()
											.setIdFieldName("type_id")
											.setOutput(storeHandler.getSdeStore("types"));
									transformer = sdeTypeTransformerProvider.get();
									break;
								case SDE_DOGMA_ATTRIBUTES_PATH:
									storeLoader = simpleLoaderProvider
											.get()
											.setIdFieldName("attribute_id")
											.setOutput(storeHandler.getSdeStore("dogma-attributes"));
									transformer = sdeDogmaAttributesTransformerProvider.get();
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
								storeLoader.setTransformer(TransformUtil.concat(fieldRenamer, transformer));
							}
							return storeLoader.readValues(pair.getRight());
						},
						false,
						1);
	}
}
