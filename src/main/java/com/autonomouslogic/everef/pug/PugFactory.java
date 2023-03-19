package com.autonomouslogic.everef.pug;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.template.TemplateLoader;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Creates new {@link PugConfiguration} instances for Jade template rendering.
 */
@RequiredArgsConstructor
public class PugFactory extends BasePooledObjectFactory<PugConfiguration> {
	@NonNull
	private final TemplateLoader templateLoader;

	@Getter
	@Setter
	private boolean prettyPrint = false;

	@Getter
	@Setter
	private String basePath = "";

	@Getter
	@Setter
	private boolean caching = false;

	@Getter
	@Setter
	private Pug4J.Mode mode = Pug4J.Mode.HTML;

	@Getter
	@Setter
	private Map<String, Object> shared = new HashMap<>();

	@Override
	public PugConfiguration create() throws Exception {
		var config = new PugConfiguration();

		config.setTemplateLoader(templateLoader);
		config.setPrettyPrint(prettyPrint);
		// config.setBasePath(basePath); @todo this method exists in jade4j, but not pug4j
		config.setCaching(caching);
		config.setSharedVariables(shared);
		return config;
	}

	@Override
	public PooledObject<PugConfiguration> wrap(PugConfiguration obj) {
		return new DefaultPooledObject<>(obj);
	}
}
