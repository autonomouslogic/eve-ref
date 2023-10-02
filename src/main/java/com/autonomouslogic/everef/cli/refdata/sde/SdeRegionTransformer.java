package com.autonomouslogic.everef.cli.refdata.sde;

import com.autonomouslogic.everef.cli.refdata.SimpleTransformer;
import com.autonomouslogic.everef.cli.refdata.TransformUtil;
import com.autonomouslogic.everef.model.refdata.RefTypeConfig;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class SdeRegionTransformer implements SimpleTransformer {
	@Inject
	protected TransformUtil transformUtil;

	@Setter
	private RefTypeConfig typeConfig;

	@Setter
	@NonNull
	private String filename;

	@Inject
	protected SdeRegionTransformer() {}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		var matcher = typeConfig.getFileRegex().matcher(filename);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Filename " + filename + " does not match region regex");
		}
		var universeId = matcher.group("universeId");
		if (universeId == null) {
			throw new NullPointerException("No universe ID found in " + filename);
		}
		json.put("universe_id", universeId);

		transformUtil.listToCoordinate(json, "center");
		transformUtil.listToCoordinate(json, "min");
		transformUtil.listToCoordinate(json, "max");

		return json;
	}
}
