package com.autonomouslogic.everef.db;

import com.autonomouslogic.everef.config.Configs;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

@RequiredArgsConstructor
public abstract class BaseDao<T extends Table<R>, R extends UpdatableRecord<R>, P> {
	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected DbAdapter dbAdapter;

	private final int insertSize = Configs.INSERT_SIZE.getRequired();

	protected final Table<R> table;

	public void insert(P pojo) {
		insert(dbAccess.context(), pojo);
	}

	public void insert(List<P> pojos) {
		insert(dbAccess.context(), pojos);
	}

	public void insert(DSLContext ctx, P pojo) {
		insert(ctx, List.of(pojo));
	}

	public void insert(DSLContext ctx, List<P> pojos) {
		ctx.transaction(trx -> {
			Flowable.fromIterable(pojos)
					.map(this::toRecord)
					.buffer(insertSize)
					.doOnNext(records -> dbAdapter.insert(trx.dsl(), table, records))
					.ignoreElements()
					.blockingAwait();
		});
	}

	// @todo jooq can do pojo conversion, but can't add Jackson annotations nor create Jackson-compatible objects.
	public abstract P fromRecord(R record);

	public abstract R toRecord(P record);

	public List<R> toRecords(List<P> pojos) {
		return pojos.stream().map(this::toRecord).toList();
	}
}
