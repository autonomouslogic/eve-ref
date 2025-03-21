package com.autonomouslogic.everef.cli.refdata.hoboleaks;

import com.autonomouslogic.everef.cli.refdata.FieldRenamer;
import com.autonomouslogic.everef.cli.refdata.SimpleStoreLoader;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.autonomouslogic.everef.cli.refdata.TransformerBuilder;
import com.autonomouslogic.everef.cli.refdata.transformer.BlueprintTransformer;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class HoboleaksLoader {
	@Inject
	protected FieldRenamer fieldRenamer;

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected TransformerBuilder transformerBuilder;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected Provider<SimpleStoreLoader> simpleLoaderProvider;

	@Inject
	protected Provider<HoboleaksMutaplasmidTransformer> hoboleaksMutiplasmidTransformerProvider;

	@Inject
	protected Provider<BlueprintTransformer> blueprintTransformerProvider;

	@Inject
	protected Provider<HoboleaksIndustryModifierSourcesLoader> hoboleaksIndustryModifierSourcesLoaderProvider;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Inject
	protected HoboleaksLoader() {}

	public Completable load(@NonNull File file) {
		var modifierSourcesLoader =
				hoboleaksIndustryModifierSourcesLoaderProvider.get().setStoreHandler(storeHandler);
		return CompressUtil.loadArchive(file)
				.flatMapCompletable(
						pair -> {
							var filename = pair.getLeft().getName();
							var bytes = pair.getRight();
							switch (filename) {
								case "industrymodifiersources.json":
									modifierSourcesLoader.setModifierSources((ObjectNode) objectMapper.readTree(bytes));
									return modifierSourcesLoader.load();
								case "industrytargetfilters.json":
									modifierSourcesLoader.setTargetFilters((ObjectNode) objectMapper.readTree(bytes));
									return modifierSourcesLoader.load();
							}
							var config = refDataUtil.getHoboleaksConfigForFilename(filename);
							if (config == null) {
								return Completable.complete();
							}
							var transformer = transformerBuilder.buildTransformer(config.getHoboleaks());
							var storeLoader = simpleLoaderProvider
									.get()
									.setFormat("json")
									.setIdFieldName(config.getIdField())
									.setOutput(storeHandler.getHoboleaksStore(config.getId()));
							switch (config.getId()) {
								case "mutaplasmids":
									transformer = TransformUtil.concat(
											transformer, hoboleaksMutiplasmidTransformerProvider.get());
									break;
								case "blueprints":
									transformer = TransformUtil.concat(transformer, blueprintTransformerProvider.get());
									break;
							}
							storeLoader.setTransformer(TransformUtil.concat(fieldRenamer, transformer));
							return storeLoader.readValues(bytes, config.getHoboleaks());
						},
						false,
						1);
	}
}
