package com.autonomouslogic.everef.cli.refdata;

import tools.jackson.databind.node.ObjectNode;

public interface SimpleTransformer {
	ObjectNode transformJson(ObjectNode json, String language) throws Throwable;
}
