package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.FieldRenamer;
import com.autonomouslogic.everef.cli.refdata.SimpleStoreLoader;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.autonomouslogic.everef.cli.refdata.TransformerBuilder;
import com.autonomouslogic.everef.cli.refdata.transformer.BlueprintTransformer;
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
	@Inject
	protected FieldRenamer fieldRenamer;

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected TransformerBuilder transformerBuilder;

	@Inject
	protected Provider<SimpleStoreLoader> simpleLoaderProvider;

	@Inject
	protected Provider<SdeTypeTransformer> sdeTypeTransformerProvider;

	@Inject
	protected Provider<BlueprintTransformer> blueprintTransformerProvider;

	@Inject
	protected Provider<SdeDogmaEffectTransformer> sdeDogmaEffectTransformerProvider;

	@Inject
	protected Provider<SchematicTransformer> schematicTransformerProvider;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Inject
	protected SdeLoader() {}

	public Completable load(@NonNull File file) {
		return CompressUtil.loadArchive(file)
				.flatMapCompletable(
						pair -> {
							var config = refDataUtil.getSdeConfigForFilename(
									pair.getLeft().getName());
							if (config == null) {
								return Completable.complete();
							}
							var transformer = transformerBuilder.buildTransformer(config.getSde());
							var storeLoader = simpleLoaderProvider
									.get()
									.setIdFieldName(config.getIdField())
									.setOutput(storeHandler.getSdeStore(config.getId()));
							switch (config.getId()) {
								case "types":
									transformer = TransformUtil.concat(transformer, sdeTypeTransformerProvider.get());
									break;
								case "blueprints":
									transformer = TransformUtil.concat(transformer, blueprintTransformerProvider.get());
									break;
								case "dogmaEffects":
									transformer =
											TransformUtil.concat(transformer, sdeDogmaEffectTransformerProvider.get());
									break;
								case "schematics":
									transformer = TransformUtil.concat(transformer, schematicTransformerProvider.get());
									break;
							}
							storeLoader.setTransformer(TransformUtil.concat(fieldRenamer, transformer));
							return storeLoader.readValues(pair.getRight(), config.getSde());
						},
						false,
						1);
	}
}
