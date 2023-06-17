package com.autonomouslogic.everef.cli.refdata.esi;

import com.autonomouslogic.everef.cli.refdata.SimpleStoreLoader;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.cli.refdata.TransformerBuilder;
import com.autonomouslogic.everef.model.refdata.RefDataConfig;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
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
	protected TransformerBuilder transformerBuilder;

	@Inject
	protected Provider<SimpleStoreLoader> simpleStoreLoaderProvider;

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
							var fileType = resolveFileType(pair.getLeft().getName());
							if (fileType == null) {
								return Completable.complete();
							}
							var config = fileType.getRefTypeConfig();
							var storeLoader = simpleStoreLoaderProvider.get();
							storeLoader
									.setIdFieldName(config.getIdField())
									.setOutput(storeHandler.getEsiStore(config.getId()));
							var transformer = transformerBuilder.buildTransformer(config.getEsi());
							storeLoader.setTransformer(transformer);
							storeLoader.setPostMergeTransformer(esiFieldOrderTransformerProvider.get());
							storeLoader.setLanguage(fileType.getLanguage());
							return storeLoader.readValues(pair.getRight());
						},
						false,
						1);
	}

	public EsiFileType resolveFileType(String filename) {
		var fileType = getFileType(filename);
		if (fileType == null) {
			return null;
		}
		var language = getLanguage(filename);
		var config = refDataUtil.getEsiConfigForFilename(fileType);
		if (config == null) {
			return null;
		}
		return EsiFileType.builder()
				.fileType(fileType)
				.language(language)
				.refTypeConfig(config)
				.build();
	}

	public String getFileType(String filename) {
		var matcher = FILE_PATTERN.matcher(filename);
		if (!matcher.matches()) {
			return null;
		}
		var match = matcher.group(1);
		return match;
	}

	public String getLanguage(String filename) {
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

	@Value
	@Builder
	public static class EsiFileType {
		String fileType;
		String language;
		RefDataConfig refTypeConfig;
	}
}
