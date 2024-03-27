package com.autonomouslogic.everef.cli.structures;

import io.reactivex.rxjava3.core.Flowable;

public interface StructureSource {
	Flowable<Long> getStructures();

	void setStructureStore(StructureStore structureStore);
}
