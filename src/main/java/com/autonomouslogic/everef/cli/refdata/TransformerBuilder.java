package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.everef.model.refdata.RefTypeConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
		return TransformUtil.concat(transformers.toArray(new SimpleTransformer[0]));
	}

	private List<SimpleTransformer> buildRenameTransformers(RefTypeConfig config) {
		var transformers = new ArrayList<SimpleTransformer>();
		if (config.getRenames() != null) {
			for (Map.Entry<String, String> entry : config.getRenames().entrySet()) {
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
			for (String attribute : config.getLanguageAttributes()) {
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
			for (Map.Entry<String, String> entry : config.getArrayToObjects().entrySet()) {
				transformers.add((json, language) -> {
					transformUtil.arrayToObject(json, entry.getKey(), entry.getValue());
					return json;
				});
			}
		}
		return transformers;
	}
}
