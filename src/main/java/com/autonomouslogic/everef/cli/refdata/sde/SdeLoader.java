package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.FieldRenamer;
import com.autonomouslogic.everef.cli.refdata.SimpleStoreLoader;
import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
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
	protected RefDataUtil refDataUtil;

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
							SimpleTransformer transformer = null;
							var config = refDataUtil.getSdeConfigForFilename(
									pair.getLeft().getName());
							if (config == null) {
								return Completable.complete();
							}
							var storeLoader = simpleLoaderProvider
									.get()
									.setIdFieldName(config.getIdField())
									.setOutput(storeHandler.getSdeStore(config.getId()));
							switch (config.getId()) {
								case "types":
									transformer = sdeTypeTransformerProvider.get();
									break;
								case "dogmaAttributes":
									transformer = sdeDogmaAttributesTransformerProvider.get();
									break;
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
