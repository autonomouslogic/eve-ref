package com.autonomouslogic.everef.refdata.sde;

import com.autonomouslogic.everef.refdata.FieldRenamer;
import com.autonomouslogic.everef.util.CompressUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads entries from the SDE dumps and prepares them for Ref Data.
 */
@Log4j2
public class SdeLoader {
	@Inject
	@Named("yaml")
	protected ObjectMapper yamlMapper;
	@Inject
	protected FieldRenamer fieldRenamer;

	@Setter
	@NonNull
	private Map<Long, JsonNode> typeStore;

	@Inject
	protected SdeLoader() {}

	public Completable load(@NonNull File file) {
		return CompressUtil.loadArchive(file)
			.flatMapCompletable(pair -> {
				switch (pair.getLeft().getName()) {
					case "sde/fsd/typeIDs.yaml":
						return readValues(pair.getRight(), typeStore, "type_id");
					default:
						log.warn("Unknown SDE entry: {}", pair.getLeft().getName());
						break;
				}
				return Completable.complete();
			});
	}

	@SneakyThrows
	private Completable readValues(@NonNull byte[] bytes, @NonNull Map<Long, JsonNode> store, @NonNull String idField) {
		return Completable.fromAction(() -> {
			var container = (ObjectNode) yamlMapper.readTree(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
			container.fields().forEachRemaining(entry -> {
				var id = Long.parseLong(entry.getKey());
				var node = fieldRenamer.fieldRenameFromSde(entry.getValue());
				((ObjectNode) node).put(idField, id);
				store.put(id, node);
			});
		})
			.subscribeOn(Schedulers.computation());
	}
}
