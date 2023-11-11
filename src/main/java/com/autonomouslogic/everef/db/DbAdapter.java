package com.autonomouslogic.everef.db;

import com.autonomouslogic.everef.config.Configs;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

@Singleton
@Log4j2
public class DbAdapter {
	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected DbAdapter() {}

	public Table<?> prefixTable(Table<?> table) {
		var prefix = Configs.DATABASE_TABLE_NAME_PREFIX.getRequired();
		return null; // @todo
	}

	public <R extends UpdatableRecord<R>> void insert(Table<R> table, List<R> records) {
		insert(dbAccess.context(), table, records);
	}

	public <R extends UpdatableRecord<R>> void insert(DSLContext ctx, Table<R> table, List<R> records) {
		var stmt = ctx.insertInto(table).columns(table.fields());
		for (var record : records) {
			stmt = stmt.values(record);
		}
		stmt.onDuplicateKeyIgnore().execute();
		log.debug("Inserted {} records into {}", records.size(), table.getName());
	}
}
