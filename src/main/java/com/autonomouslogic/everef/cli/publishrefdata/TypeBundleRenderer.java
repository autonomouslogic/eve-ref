package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.refdata.DogmaTypeAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;

/**
 * Renders the basic objects in the reference data collections.
 */
@Log4j2
public class TypeBundleRenderer implements RefDataRenderer {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected RefDataUtil refDataUtil;

	@Setter
	@NonNull
	private MVStore dataStore;

	@Inject
	protected TypeBundleRenderer() {}

	public Flowable<Pair<String, JsonNode>> render() {
		return Flowable.defer(() -> {
			log.info("Creating type bundles");
			var types = mvStoreUtil.openJsonMap(dataStore, "types", Long.class);
			var dogma = mvStoreUtil.openJsonMap(dataStore, "dogma_attributes", Long.class);
			return Flowable.fromIterable(types.keySet()).flatMapMaybe(typeId -> createBundle(types.get(typeId), dogma));
		});
	}

	private Maybe<Pair<String, JsonNode>> createBundle(JsonNode type, Map<Long, JsonNode> dogma) {
		var parsedType = objectMapper.convertValue(type, InventoryType.class);

		var path = refDataUtil.subPath("types", parsedType.getTypeId()) + "/bundle";

		var bundle = objectMapper.createObjectNode();
		var types = bundle.putObject("types");
		var dogmaAttributes = objectMapper.createObjectNode();

		types.set(Long.toString(parsedType.getTypeId()), type);

		Optional.ofNullable(parsedType.getDogmaAttributes()).stream()
				.flatMap(e -> e.values().stream())
				.map(DogmaTypeAttribute::getAttributeId)
				.forEach(typeAttr -> {
					var attr = dogma.get(typeAttr);
					if (attr != null) {
						dogmaAttributes.set(Long.toString(typeAttr), attr);
					}
				});

		var valid = false;
		if (!dogmaAttributes.isEmpty()) {
			bundle.set("dogma_attributes", dogmaAttributes);
			valid = true;
		}

		if (valid) {
			return Maybe.just(Pair.of(path, bundle));
		}
		return Maybe.empty();
	}
}
