package com.autonomouslogic.everef.db;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jooq.DSLContext;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.impl.DSL;

@Singleton
public class DbAccess {
	private final String url = Configs.DATABASE_URL.getRequired();
	private final String username = Configs.DATABASE_USERNAME.getRequired();
	private final String password = Configs.DATABASE_PASSWORD.getRequired();

	private volatile Flyway flywayInstance;
	private volatile DSLContext contextInstance;

	@Inject
	protected DbAccess() {}

	public Flyway flyway() {
		if (flywayInstance == null) {
			synchronized (this) {
				if (flywayInstance == null) {
					var locations = "classpath:com/autonomouslogic/everef/db/migrations";
					var config = Flyway.configure()
							.dataSource(url, username, password)
							.locations(locations);
					configFlywayTable(config);
					flywayInstance = config.load();
				}
			}
		}
		return flywayInstance;
	}

	public DSLContext context() {
		if (contextInstance == null) {
			synchronized (this) {
				if (contextInstance == null) {
					contextInstance = DSL.using(url, username, password);
					var settings = contextInstance.settings();
					settings.setRenderQuotedNames(RenderQuotedNames.ALWAYS);
					settings.setRenderNameStyle(RenderNameStyle.QUOTED);
				}
			}
		}
		return contextInstance;
	}

	private void configFlywayTable(FluentConfiguration conf) {
		var prefix = Configs.DATABASE_TABLE_NAME_PREFIX.getRequired();
		var defaultTable = conf.getTable();
		conf.table(prefix + defaultTable);
	}

	public String getTableName(Class<?> clazz) {
		var prefix = Configs.DATABASE_TABLE_NAME_PREFIX.getRequired();
		if (clazz.equals(MarketHistoryEntry.class)) {
			return prefix + "market_history";
		}
		throw new IllegalArgumentException("Unknown class: " + clazz.getName());
	}
}
