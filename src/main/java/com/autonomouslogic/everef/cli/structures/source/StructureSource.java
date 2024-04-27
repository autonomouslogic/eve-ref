package com.autonomouslogic.everef.cli.structures.source;

import com.autonomouslogic.everef.cli.structures.StructureStore;
import io.reactivex.rxjava3.core.Flowable;

public interface StructureSource {
	Flowable<Long> getStructures();

	void setStructureStore(StructureStore structureStore);
}
