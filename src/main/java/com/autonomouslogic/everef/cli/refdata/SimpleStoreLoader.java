package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.everef.model.refdata.RefTypeConfig;
import io.reactivex.rxjava3.core.Completable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads big objects from a YAML file, transforms and converts them and stores them in the target map.
 */
@Log4j2
public class SimpleStoreLoader {
	@Inject
	@Named("yaml")
	protected ObjectMapper yamlMapper;

	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected ObjectMerger objectMerger;

	@Setter
	@NonNull
	private String format = "yaml";

	@Setter
	@NonNull
	private Map<Long, JsonNode> output;

	@Setter
	@NonNull
	private SimpleTransformer transformer;

	@Setter
	@NonNull
	private SimpleTransformer postMergeTransformer;

	@Setter
	private String idFieldName;

	@Setter
	@NonNull
	private String language = "en";

	@Inject
	protected SimpleStoreLoader() {}

	@SneakyThrows
	public Completable readValues(@NonNull byte[] bytes, RefTypeConfig refTypeConfig) {
		return Completable.fromAction(() -> {
			var container = readContainer(bytes);
			if (refTypeConfig.isIndividualFiles()) {
				var transformed = transform(container);
				var id = transformed.get(idFieldName).asLong();
				writeEntry(transformed, id);
			} else {
				container.properties().forEach(entry -> {
					var id = Long.parseLong(entry.getKey());
					var val = entry.getValue();
					if (val == null || val.isNull()) {
						return;
					}
					var json = (ObjectNode) val;
					var transformed = transform(json);
					writeEntry(transformed, id);
				});
			}
		});
	}

	@SneakyThrows
	private void writeEntry(ObjectNode json, long id) {
		handleIdField(json, id);
		if (output.containsKey(id)) {
			var existing = (ObjectNode) output.get(id);
			json = (ObjectNode) objectMerger.merge(existing, json);
			if (postMergeTransformer != null) {
				json = postMergeTransformer.transformJson(json, language);
			}
		}
		output.put(id, json);
	}

	private ObjectNode readContainer(@NonNull byte[] bytes) throws IOException {
		ObjectMapper mapper;
		if (format.equals("yaml")) {
			mapper = yamlMapper;
		} else if (format.equals("json")) {
			mapper = jsonMapper;
		} else {
			throw new RuntimeException(format);
		}
		var container = (ObjectNode) mapper.readTree(new ByteArrayInputStream(bytes));
		return container;
	}

	@SneakyThrows
	private ObjectNode transform(ObjectNode json) {
		var transformed = json;
		if (transformer != null) {
			transformed = transformer.transformJson(json, language);
		}
		return transformed;
	}

	protected void handleIdField(ObjectNode json, long id) {
		if (idFieldName != null) {
			json.put(idFieldName, id);
		}
	}
}
