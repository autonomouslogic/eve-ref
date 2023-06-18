package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;

/**
 * Reads big objects from a YAML file, transforms and converts them and stores them in the target map.
 */
@Log4j2
public class SimpleStoreLoader {
	@Inject
	@Named("yaml")
	protected ObjectMapper yamlMapper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected ObjectMerger objectMerger;

	@Setter
	@NonNull
	private String format = "yaml";

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> output;

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
	public Completable readValues(@NonNull byte[] bytes) {
		return Completable.defer(() -> {
					ObjectNode container = readContainer(bytes);
					return Flowable.fromIterable(() -> container.fields()).flatMapCompletable(entry -> {
						var id = Long.parseLong(entry.getKey());
						var json = (ObjectNode) entry.getValue();
						json = readValue(id, json);
						if (output.containsKey(id)) {
							var existing = (ObjectNode) output.get(id);
							json = (ObjectNode) objectMerger.merge(existing, json);
							if (postMergeTransformer != null) {
								json = postMergeTransformer.transformJson(json, language);
							}
						}
						output.put(id, json);
						return Completable.complete();
					});
				})
				.subscribeOn(Schedulers.computation());
	}

	private ObjectNode readContainer(@NonNull byte[] bytes) throws IOException {
		ObjectMapper mapper;
		if (format.equals("yaml")) {
			mapper = yamlMapper;
		} else if (format.equals("json")) {
			mapper = objectMapper;
		} else {
			throw new RuntimeException(format);
		}
		var container = (ObjectNode) mapper.readTree(new ByteArrayInputStream(bytes));
		return container;
	}

	@SneakyThrows
	protected ObjectNode readValue(long id, ObjectNode json) {
		var transformed = json;
		if (transformer != null) {
			transformed = transformer.transformJson(json, language);
		}
		handleIdField(transformed, id);
		return transformed;
	}

	protected void handleIdField(ObjectNode json, long id) {
		if (idFieldName != null) {
			json.put(idFieldName, id);
		}
	}
}
