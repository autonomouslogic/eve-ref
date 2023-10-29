package com.autonomouslogic.everef.db;

import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

@RequiredArgsConstructor
public abstract class BaseDao<T extends Table<R>, R extends UpdatableRecord<R>, P> {
	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected DbAdapter dbAdapter;

	protected final Table<R> table;

	public Completable insert(P pojo) {
		return insert(List.of(pojo));
	}

	public Completable insert(List<P> pojos) {
		return dbAdapter.insert(table, toRecords(pojos));
	}

	// @todo jooq can do pojo conversion, but can't add Jackson annotations nor create Jackson-compatible objects.
	public abstract P fromRecord(R record);

	public abstract R toRecord(P record);

	public List<R> toRecords(List<P> pojos) {
		return pojos.stream().map(this::toRecord).toList();
	}
}
