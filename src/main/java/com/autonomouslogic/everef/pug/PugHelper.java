package com.autonomouslogic.everef.pug;

import com.autonomouslogic.everef.util.LocalFileHelper;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.template.PugTemplate;
import java.io.File;
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
	@Inject
	protected LocalFileHelper localFileHelper;

	private final PugFactory pugFactory;
	private final ObjectPool<PugConfiguration> configPool;

	@Inject
	protected PugHelper() {
		pugFactory = new PugFactory();
		var shared = pugFactory.getShared();
		shared.put("format", new NumberFormats());
		shared.put("time", new TimeUtil());
		var poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMaxTotal(1024);
		configPool = new GenericObjectPool<>(pugFactory, poolConfig);
	}

	public byte[] renderTemplate(String template, Map<String, Object> model) {
		PugConfiguration config = null;
		try {
			config = configPool.borrowObject();
			if (model == null) model = Collections.emptyMap();
			StringBuilderWriter writer = new StringBuilderWriter();
			config.renderTemplate(getTemplate(config, template), model, writer);
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

	public PugTemplate getTemplate(PugConfiguration config, String template) {
		try {
			File templateFile = localFileHelper.getTemplate(template);
			return config.getTemplate(templateFile.toString());
		} catch (Throwable e) {
			throw new RuntimeException("Failed loading template: " + template, e);
		}
	}
}
