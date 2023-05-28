package com.autonomouslogic.everef.cli.refdata.esi;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.util.CompressUtil;
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
							var language = getLanguage(pair.getLeft().getName());
							EsiStoreLoader storeLoader = null;
							if (fileType == null) {
								log.warn("Unknown ESI entry: {}", pair.getLeft().getName());
								return Completable.complete();
							}
							switch (fileType) {
								case "types":
									storeLoader = esiStoreLoaderProvider
											.get()
											.setEsiTransformer(esiTypeTransformerProvider.get());
									storeLoader.setIdFieldName("type_id").setOutput(storeHandler.getEsiStore("types"));
									break;
								case "dogma-attributes":
									storeLoader = esiStoreLoaderProvider
											.get()
											.setEsiTransformer(esiDogmaAttributesTransformerProvider.get());
									storeLoader
											.setIdFieldName("attribute_id")
											.setOutput(storeHandler.getEsiStore("dogma-attributes"));
									break;
								default:
									log.warn(
											"Unknown ESI entry type {}: {}",
											fileType,
											pair.getLeft().getName());
									return Completable.complete();
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
		var mapped = mapFileType(match);
		return mapped;
	}

	private String mapFileType(String type) {
		if (type.startsWith("universe/")) {
			return type.split("/")[1];
		}
		if (type.startsWith("dogma/")) {
			return type.replace('/', '-');
		}
		return type;
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
