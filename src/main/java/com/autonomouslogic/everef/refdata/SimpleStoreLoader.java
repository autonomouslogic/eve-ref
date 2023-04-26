package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.ByteArrayInputStream;
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

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> output;

	@Setter
	@NonNull
	private Function<ObjectNode, ObjectNode> transformer;

	@Setter
	@NonNull
	private String idFieldName;

	@Inject
	protected SimpleStoreLoader() {}

	@SneakyThrows
	public Completable readValues(@NonNull byte[] bytes) {
		return Completable.defer(() -> {
					var container = (ObjectNode) yamlMapper.readTree(new ByteArrayInputStream(bytes));
					return Flowable.fromIterable(() -> container.fields()).flatMapCompletable(entry -> {
						var id = Long.parseLong(entry.getKey());
						var json = (ObjectNode) entry.getValue();
						return readValue(id, json);
					});
				})
				.subscribeOn(Schedulers.computation());
	}

	protected Completable readValue(long id, ObjectNode json) {
		return Completable.fromAction(() -> {
			var transformed = json;
			if (transformer != null) {
				transformed = transformer.apply(json);
			}
			handleIdField(transformed, id);
			output.put(id, transformed);
		});
	}

	protected void handleIdField(ObjectNode json, long id) {
		if (idFieldName != null) {
			json.put(idFieldName, id);
		}
	}
}
