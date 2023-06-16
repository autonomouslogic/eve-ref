package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.cli.refdata.BuildRefData;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.log4j.Log4j2;

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
	protected BuildRefData buildRefData;

	@Inject
	protected ImportTestResources() {}

	public Completable run() {
		if (!new File(TEST_RESOURCES).exists()) {
			throw new RuntimeException("Test resources directory does not exist");
		}
		return dataUtil.downloadLatestSde().flatMapCompletable(this::loadSdeResources);
	}

	private Completable loadSdeResources(File file) {
		return CompressUtil.loadArchive(file).flatMapCompletable(pair -> {
			var entry = pair.getLeft();
			var config = refDataUtil.getSdeConfigForFilename(entry.getName());
			if (config == null || config.getSde() == null) {
				return Completable.complete();
			}
			var content = (ObjectNode) yamlMapper.readTree(pair.getRight());
			var newContent = yamlMapper.createObjectNode();
			for (Long id : config.getTest().getIds()) {
				var stringId = id.toString();
				newContent.put(stringId, content.get(stringId));
			}
			var outputFile = new File(REFDATA_RESOURCES + "/" + entry.getName());
			log.info("Writing {}", outputFile);
			yamlMapper.writeValue(outputFile, newContent);
			return Completable.complete();
		});
	}
}
