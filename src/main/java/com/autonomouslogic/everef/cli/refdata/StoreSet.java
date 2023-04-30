package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import org.h2.mvstore.MVMap;

@Value
public class StoreSet {
	MVMap<Long, JsonNode> sdeStore;
	MVMap<Long, JsonNode> esiStore;
	MVMap<Long, JsonNode> refStore;
}
