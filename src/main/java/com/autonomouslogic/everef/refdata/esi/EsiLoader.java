package com.autonomouslogic.everef.refdata.esi;

import com.autonomouslogic.everef.util.CompressUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import java.util.regex.Pattern;
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
	private static final Pattern FILE_PATTERN = Pattern.compile(".*?([^\\.\\/]+)\\.([^\\.]+)\\.yaml");

	@Inject
	protected Provider<EsiStoreLoader> esiStoreLoaderProvider;

	@Inject
	protected Provider<EsiTypeTransformer> esiTypeTransformerProvider;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> typeStore;

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
									storeLoader.setIdFieldName("type_id").setOutput(typeStore);
									break;
								default:
									log.warn(
											"Unknown ESI entry type {}: {}",
											fileType,
											pair.getLeft().getName());
									return Completable.complete();
							}
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
		return matcher.group(1);
	}

	protected String getLanguage(String filename) {
		var matcher = FILE_PATTERN.matcher(filename);
		if (!matcher.matches()) {
			return null;
		}
		var lang = matcher.group(2);
		if (lang.equals("en-us")) {
			return "en";
		}
		return lang;
	}
}
