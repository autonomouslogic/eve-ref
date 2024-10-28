package com.autonomouslogic.everef.cli.refdata.hoboleaks;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.Setter;

import javax.inject.Inject;

/**
 * Reads the Hoboleaks industry modifier sources to create entries on the rig types for affected types.
 */
public class HoboleaksIndustryModifierSourcesLoader {
	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Setter
	@NonNull
	private ObjectNode modifierSources;

	@Setter
	@NonNull
	private ObjectNode targetFilters;

	@Inject
	public HoboleaksIndustryModifierSourcesLoader() {}
}
