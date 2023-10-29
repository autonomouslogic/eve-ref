package com.autonomouslogic.everef.db;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

@Singleton
public class DbAdapter {
	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected DbAdapter() {}

	public Table<?> prefixTable(Table<?> table) {
		var prefix = Configs.DATABASE_TABLE_NAME_PREFIX.getRequired();
		return null; // @todo
	}

	public <R extends UpdatableRecord<R>> Completable insert(Table<R> table, List<R> records) {
		return Completable.fromAction(() -> {
					var stmt = dbAccess.context()
							// .insertInto(DSL.name(prefixTable(table)))
							.insertInto(table)
							.columns(table.fields());
					for (var record : records) {
						stmt = stmt.values(record);
					}
					stmt.onDuplicateKeyIgnore().execute();
				})
				.compose(Rx.offloadCompletable());
	}
}
