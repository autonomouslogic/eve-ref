package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.cli.refdata.BuildRefData;
import com.autonomouslogic.everef.cli.refdata.FieldRenamer;
import com.autonomouslogic.everef.cli.refdata.ObjectMerger;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.cli.refdata.esi.EsiLoader;
import com.autonomouslogic.everef.model.refdata.RefDataConfig;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

/**
 * Loads the resources configured in <code>refdata.yaml</code> and imports those resources fro the SDE and ESI.
 * Also prepares the expected outputs by running them through the same processing as for the reference data.
 * This makes it easy to add new resources for testing, while still being able to verify any changes to the output
 * is correct. It also makes it easy to update for new in the input data.
 */
@Log4j2
public class ImportTestResources implements Command {
	private static final String TEST_RESOURCES = "src/test/resources";
	private static final String REFDATA_RESOURCES = TEST_RESOURCES + "/refdata/";
	private static final String HOBOLEAKS_RESOURCES = REFDATA_RESOURCES + "/hoboleaks/";

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	@Named("yaml")
	protected ObjectMapper yamlMapper;

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected DataUtil dataUtil;

	@Inject
	protected EsiLoader esiLoader;

	@Inject
	protected BuildRefData buildRefData;

	@Inject
	protected MockScrapeBuilder mockScrapeBuilder;

	@Inject
	protected FieldRenamer fieldRenamer;

	@Inject
	protected ObjectMerger objectMerger;

	@Inject
	protected ImportTestResources() {}

	public void run() {
		if (!new File(TEST_RESOURCES).exists()) {
			throw new RuntimeException("Test resources directory does not exist");
		}

		var sdeFile = dataUtil.downloadLatestSde().blockingGet();
		loadSdeResources(sdeFile).blockingAwait();

		var esiFile = dataUtil.downloadLatestEsi();
		loadEsiResources(esiFile).blockingAwait();

		var hoboleaksFile = dataUtil.downloadLatestHoboleaks();
		loadHoboleaksResources(hoboleaksFile).blockingAwait();

		buildRefData().blockingAwait();
	}

	private Completable loadSdeResources(File file) {
		return CompressUtil.loadArchive(file).flatMapCompletable(pair -> {
			var entry = pair.getLeft();
			var config = refDataUtil.getSdeConfigForFilename(entry.getName());
			if (config == null || config.getSde() == null) {
				return Completable.complete();
			}
			var content = (ObjectNode) yamlMapper.readTree(pair.getRight());
			byte[] output;
			if (config.getSde().isIndividualFiles()) {
				var renamed = fieldRenamer.fieldRenameFromSde(content);
				var id = renamed.get(config.getIdField()).asLong();
				if (!config.getTest().getIds().contains(id)) {
					return Completable.complete();
				}
				output = pair.getRight();
			} else {
				var newContent = yamlMapper.createObjectNode();
				for (Long id : config.getTest().getIds()) {
					var stringId = id.toString();
					newContent.put(stringId, content.get(stringId));
				}
				output = yamlMapper.writeValueAsBytes(newContent);
			}
			var outputFile = new File(REFDATA_RESOURCES + "/sde/" + entry.getName());
			outputFile.getParentFile().mkdirs();
			log.info("Writing {}", outputFile);
			try (var out = new FileOutputStream(outputFile)) {
				IOUtils.write(output, out);
			}
			return Completable.complete();
		});
	}

	private Completable loadHoboleaksResources(File file) {
		return CompressUtil.loadArchive(file).flatMapCompletable(pair -> {
			var prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();
			var entry = pair.getLeft();
			var config = refDataUtil.getHoboleaksConfigForFilename(entry.getName());
			if (config == null || config.getHoboleaks() == null) {
				return Completable.complete();
			}
			if (config.getHoboleaks().isIndividualFiles()) {
				throw new IllegalArgumentException("Individual files not supported for Hoboleaks.");
			}
			var content = (ObjectNode) objectMapper.readTree(pair.getRight());
			var newContent = objectMapper.createObjectNode();
			for (Long id : config.getTest().getIds()) {
				var stringId = id.toString();
				newContent.put(stringId, content.get(stringId));
			}
			var outputFile = new File(HOBOLEAKS_RESOURCES + "/" + entry.getName());
			log.info("Writing {}", outputFile);
			prettyPrinter.writeValue(outputFile, newContent);
			return Completable.complete();
		});
	}

	private Completable loadEsiResources(File file) {
		return CompressUtil.loadArchive(file).flatMapCompletable(pair -> {
			var entry = pair.getLeft();
			var fileType = esiLoader.resolveFileType(entry.getName());
			if (fileType == null) {
				return Completable.complete();
			}
			var config = fileType.getRefTypeConfig();
			if (config.getEsi() == null) {
				return Completable.complete();
			}
			if (config.getEsi().isIndividualFiles()) {
				throw new IllegalArgumentException("Individual files not supported for ESI.");
			}
			var includedLanguages = config.getTest().getLanguages();
			if (includedLanguages != null) {
				if (fileType.getLanguage().equals("en")) {
					if (!includedLanguages.contains("en-us")) {
						return Completable.complete();
					}
				} else if (!includedLanguages.contains(fileType.getLanguage())) {
					return Completable.complete();
				}
			}
			var content = (ObjectNode) yamlMapper.readTree(pair.getRight());
			var newContent = yamlMapper.createObjectNode();
			for (Long id : config.getTest().getIds()) {
				var stringId = id.toString();
				newContent.put(stringId, content.get(stringId));
			}
			var fileName = entry.getName();
			fileName = fileName.replace("eve-ref-esi-scrape", "esi");
			var outputFile = new File(REFDATA_RESOURCES + "/" + fileName);
			log.info("Writing {}", outputFile);
			yamlMapper.writeValue(outputFile, newContent);
			return Completable.complete();
		});
	}

	private Completable buildRefData() {
		return Completable.defer(() -> {
			mockScrapeBuilder.setBasePath(TEST_RESOURCES);
			var sdeFile = mockScrapeBuilder.createTestSde();
			var esiFile = mockScrapeBuilder.createTestEsiDump();
			var hoboleaksFile = mockScrapeBuilder.createTestHoboleaksSde();
			return buildRefData
					.setSdeFile(sdeFile)
					.setEsiFile(esiFile)
					.setHoboleaksFile(hoboleaksFile)
					.setStopAtUpload(true)
					.runAsync()
					.andThen(Completable.defer(() -> {
						var storeHandler = buildRefData.getStoreHandler();
						return Completable.concatArray(
								exportEsiResources(storeHandler),
								exportSdeResources(storeHandler),
								exportHoboleaksResources(storeHandler),
								exportRefdataResources(storeHandler));
					}))
					.andThen(buildRefData.closeMvStore());
		});
	}

	private Completable exportEsiResources(@NonNull StoreHandler storeHandler) {
		return exportResources(
				TEST_RESOURCES + "/com/autonomouslogic/everef/cli/refdata/esi/EsiLoaderTest",
				storeHandler::getEsiStore);
	}

	private Completable exportSdeResources(@NonNull StoreHandler storeHandler) {
		return exportResources(
				TEST_RESOURCES + "/com/autonomouslogic/everef/cli/refdata/sde/SdeLoaderTest",
				storeHandler::getSdeStore);
	}

	private Completable exportHoboleaksResources(@NonNull StoreHandler storeHandler) {
		return exportResources(
				TEST_RESOURCES + "/com/autonomouslogic/everef/cli/refdata/hoboleaks/HoboleaksLoaderTest",
				storeHandler::getHoboleaksStore);
	}

	private Completable exportRefdataResources(@NonNull StoreHandler storeHandler) {
		return exportResources(TEST_RESOURCES + "/refdata/refdata", storeHandler::getRefStore);
	}

	private Completable exportResources(
			@NonNull String path, @NonNull Function<String, Map<Long, JsonNode>> storeProvider) {
		return Completable.fromAction(() -> {
			var prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();
			Map<String, JsonNode> fileNodes = new HashMap<>();
			for (RefDataConfig config : refDataUtil.loadReferenceDataConfig()) {
				var store = storeProvider.apply(config.getId());
				for (var id : config.getTest().getIds()) {
					var filename = path + "/" + config.getTest().getFilePrefix() + "-" + id + ".json";
					var json = store.get(id);
					if (json == null) {
						continue;
					}
					var existing = fileNodes.get(filename);
					json = existing == null ? json : objectMerger.merge(existing, json);
					fileNodes.put(filename, json);
				}
			}
			for (var entry : fileNodes.entrySet()) {
				prettyPrinter.writeValue(new File(entry.getKey()), entry.getValue());
			}
		});
	}
}
