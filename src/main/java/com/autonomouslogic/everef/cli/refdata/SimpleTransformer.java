package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface SimpleTransformer {
	ObjectNode transformJson(ObjectNode json) throws Throwable;
}
