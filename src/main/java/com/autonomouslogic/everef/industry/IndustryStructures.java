package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.cli.ImportIndustryResources.STRUCTURES_CONFIG;

import com.autonomouslogic.everef.model.IndustryStructure;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class IndustryStructures extends AbstractIndustryService<IndustryStructure> {
	@Inject
	protected IndustryStructures(CsvMapper csvMapper) {
		super(IndustryStructure.class, STRUCTURES_CONFIG, IndustryStructure::getTypeId, csvMapper);
	}
}
