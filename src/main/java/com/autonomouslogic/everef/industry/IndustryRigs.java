package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.cli.ImportIndustryResources.RIGS_CONFIG;

import com.autonomouslogic.everef.model.IndustryRig;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class IndustryRigs extends AbstractIndustryService<IndustryRig> {
	@Inject
	protected IndustryRigs(CsvMapper csvMapper) {
		super(IndustryRig.class, RIGS_CONFIG, IndustryRig::getTypeId, csvMapper);
	}
}
