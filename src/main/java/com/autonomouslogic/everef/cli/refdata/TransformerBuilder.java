package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.everef.model.refdata.RefTypeConfig;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransformerBuilder {
	@Inject
	protected TransformUtil transformUtil;

	@Inject
	protected TransformerBuilder() {}

	public SimpleTransformer buildTransformer(RefTypeConfig config) {
		var transformers = new ArrayList<SimpleTransformer>();
		transformers.addAll(buildRenameTransformers(config));
		transformers.addAll(buildLanguageAttributesTransformers(config));
		transformers.addAll(buildArrayToObjectsTransformers(config));
		transformers.addAll(buildRemovesTransformers(config));
		return TransformUtil.concat(transformers.toArray(new SimpleTransformer[0]));
	}

	private List<SimpleTransformer> buildRenameTransformers(RefTypeConfig config) {
		var transformers = new ArrayList<SimpleTransformer>();
		if (config.getRenames() != null) {
			for (var entry : config.getRenames().entrySet()) {
				transformers.add((json, language) -> {
					transformUtil.renameField(json, entry.getKey(), entry.getValue());
					return json;
				});
			}
		}
		return transformers;
	}

	private List<SimpleTransformer> buildLanguageAttributesTransformers(RefTypeConfig config) {
		var transformers = new ArrayList<SimpleTransformer>();
		if (config.getLanguageAttributes() != null) {
			for (var attribute : config.getLanguageAttributes()) {
				transformers.add((json, language) -> {
					transformUtil.setPath(json, json.get(attribute), attribute, language);
					return json;
				});
			}
		}
		return transformers;
	}

	private List<SimpleTransformer> buildArrayToObjectsTransformers(RefTypeConfig config) {
		var transformers = new ArrayList<SimpleTransformer>();
		if (config.getArrayToObjects() != null) {
			for (var entry : config.getArrayToObjects().entrySet()) {
				transformers.add((json, language) -> {
					transformUtil.arrayToObject(json, entry.getKey(), entry.getValue());
					return json;
				});
			}
		}
		return transformers;
	}

	private List<SimpleTransformer> buildRemovesTransformers(RefTypeConfig config) {
		var transformers = new ArrayList<SimpleTransformer>();
		if (config.getRemoves() != null) {
			for (var attribute : config.getRemoves()) {
				transformers.add((json, language) -> {
					transformUtil.remove(json, attribute);
					return json;
				});
			}
		}
		return transformers;
	}
}
