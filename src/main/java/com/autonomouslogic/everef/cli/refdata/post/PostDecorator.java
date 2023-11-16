package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import io.reactivex.rxjava3.core.Completable;
import lombok.NonNull;
import lombok.Setter;

public abstract class PostDecorator {
	@Setter
	@NonNull
	protected StoreHandler storeHandler;

	public abstract Completable create();
}
