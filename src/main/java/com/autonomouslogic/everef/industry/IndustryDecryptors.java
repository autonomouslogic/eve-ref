package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.cli.ImportIndustryResources.DECRYPTORS_CONFIG;

import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class IndustryDecryptors extends AbstractIndustryService<IndustryDecryptor> {
	@Inject
	protected IndustryDecryptors(CsvMapper csvMapper) {
		super(IndustryDecryptor.class, DECRYPTORS_CONFIG, IndustryDecryptor::getTypeId, csvMapper);
	}

	public int getBlueprintMe(IndustryDecryptor decryptor) {
		return IndustryConstants.INVENTION_BASE_ME + decryptor.getMeModifier();
	}

	public int getBlueprintTe(IndustryDecryptor decryptor) {
		return IndustryConstants.INVENTION_BASE_TE + decryptor.getTeModifier();
	}
}
