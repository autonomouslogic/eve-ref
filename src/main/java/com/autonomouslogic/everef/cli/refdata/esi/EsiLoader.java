package com.autonomouslogic.everef.cli.refdata.esi;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Loads entries from the ESI dumps and prepares them for Ref Data.
 */
@Log4j2
public class EsiLoader {
	private static final Pattern FILE_PATTERN = Pattern.compile(".*?([^\\.\\/]+\\/[^\\.\\/]+)(?:\\.([^\\.]+))?\\.yaml");

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected Provider<EsiStoreLoader> esiStoreLoaderProvider;

	@Inject
	protected Provider<EsiTypeTransformer> esiTypeTransformerProvider;

	@Inject
	protected Provider<EsiDogmaAttributesTransformer> esiDogmaAttributesTransformerProvider;

	@Inject
	protected Provider<EsiFieldOrderTransformer> esiFieldOrderTransformerProvider;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Inject
	protected EsiLoader() {}

	public Completable load(@NonNull File file) {
		return CompressUtil.loadArchive(file)
				.flatMapCompletable(
						pair -> {
							var fileType = getFileType(pair.getLeft().getName());
							if (fileType == null) {
								return Completable.complete();
							}
							var language = getLanguage(pair.getLeft().getName());
							var config = refDataUtil.getEsiConfigForFilename(fileType);
							if (config == null) {
								return Completable.complete();
							}
							var storeLoader = esiStoreLoaderProvider.get();
							storeLoader
									.setIdFieldName(config.getIdField())
									.setOutput(storeHandler.getEsiStore(config.getId()));
							switch (config.getId()) {
								case "types":
									storeLoader.setEsiTransformer(esiTypeTransformerProvider.get());
									break;
								case "dogmaAttributes":
									storeLoader.setEsiTransformer(esiDogmaAttributesTransformerProvider.get());
									break;
							}
							storeLoader.setPostMergeTransformer(esiFieldOrderTransformerProvider.get());
							storeLoader.setLanguage(language);
							return storeLoader.readValues(pair.getRight());
						},
						false,
						1);
	}

	protected String getFileType(String filename) {
		var matcher = FILE_PATTERN.matcher(filename);
		if (!matcher.matches()) {
			return null;
		}
		var match = matcher.group(1);
		return match;
	}

	protected String getLanguage(String filename) {
		var matcher = FILE_PATTERN.matcher(filename);
		if (!matcher.matches()) {
			return null;
		}
		var lang = matcher.group(2);
		if (lang == null) {
			lang = "en";
		}
		if (lang.equals("en-us")) {
			return "en";
		}
		return lang;
	}
}
