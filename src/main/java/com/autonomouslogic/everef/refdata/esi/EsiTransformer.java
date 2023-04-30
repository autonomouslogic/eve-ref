package com.autonomouslogic.everef.refdata.esi;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface EsiTransformer {
	ObjectNode transformEsi(ObjectNode json, String language) throws Throwable;
}
