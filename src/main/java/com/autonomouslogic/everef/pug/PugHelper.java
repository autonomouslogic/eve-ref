package com.autonomouslogic.everef.pug;

import de.neuland.pug4j.PugConfiguration;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Wrapper for Jade templating.
 * This helper is thread-safe since it implements a shared pool of {@link PugConfiguration} instances.
 */
@Singleton
public class PugHelper {
	private final PugFactory pugFactory;
	private final ObjectPool<PugConfiguration> configPool;

	@Inject
	protected PugHelper(TemplateLoaderFactory templateLoaderFactory) {
		pugFactory = new PugFactory(templateLoaderFactory.create());
		var shared = pugFactory.getShared();
		shared.put("format", new NumberFormats());
		shared.put("time", new TimeUtil());
		shared.put("helper", new GenericHelper());
		var poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMaxTotal(1024);
		configPool = new GenericObjectPool<>(pugFactory, poolConfig);
	}

	public byte[] renderTemplate(String templateName, Map<String, Object> model) {
		PugConfiguration config = null;
		try {
			config = configPool.borrowObject();
			if (model == null) model = Collections.emptyMap();
			var writer = new StringBuilderWriter();
			var template = config.getTemplate(templateName);
			config.renderTemplate(template, model, writer);
			return writer.getBuilder().toString().getBytes();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (config != null) {
				try {
					configPool.returnObject(config);
				} catch (Throwable e) {
					try {
						configPool.invalidateObject(config);
					} catch (Throwable e1) {
						// Ignore.
					}
				}
			}
		}
	}
}
