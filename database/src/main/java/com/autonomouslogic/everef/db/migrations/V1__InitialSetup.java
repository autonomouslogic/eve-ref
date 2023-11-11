package com.autonomouslogic.everef.db.migrations;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class V1__InitialSetup extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		//		var prefix = Configs.DATABASE_TABLE_NAME_PREFIX.getRequired(); // @todo
		var prefix = "";
		var marketHistory = prefix + "market_history";
		DSL.using(context.getConnection())
				.createTable(marketHistory)
				.column("date", SQLDataType.LOCALDATE.notNull())
				.column("region_id", SQLDataType.INTEGER.notNull())
				.column("type_id", SQLDataType.INTEGER.notNull())
				.column("average", SQLDataType.DECIMAL(20, 2).notNull())
				.column("highest", SQLDataType.DECIMAL(20, 2).notNull())
				.column("lowest", SQLDataType.DECIMAL(20, 2).notNull())
				.column("volume", SQLDataType.BIGINT.notNull())
				.column("order_count", SQLDataType.INTEGER.notNull())
				.column("http_last_modified", SQLDataType.INSTANT.nullable(true))
				.primaryKey("date", "region_id", "type_id")
				.execute();
		DSL.using(context.getConnection())
				.createIndex(marketHistory + "_region_id_type_id")
				.on(marketHistory, "region_id", "type_id")
				.execute();
		DSL.using(context.getConnection())
				.createIndex(marketHistory + "_type_id")
				.on(marketHistory, "type_id")
				.execute();
	}
}
