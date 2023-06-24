package com.autonomouslogic.everef.cli.refdata.post;

import io.reactivex.rxjava3.core.Completable;

public interface PostDecorator {
	Completable create();
}
