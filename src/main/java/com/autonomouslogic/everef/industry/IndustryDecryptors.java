package com.autonomouslogic.everef.industry;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.ImportIndustryResources;
import com.autonomouslogic.everef.model.Decryptor;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class IndustryDecryptors {
	private final Map<Long, Decryptor> decryptors;

	@Inject
	@SneakyThrows
	protected IndustryDecryptors(CsvMapper csvMapper) {
		var schema = csvMapper
				.schemaFor(Decryptor.class)
				.withStrictHeaders(true)
				.withColumnReordering(true)
				.withHeader();
		MappingIterator<Decryptor> iterator = null;
		try {
			iterator = csvMapper
					.readerFor(Decryptor.class)
					.with(schema)
					.readValues(ResourceUtil.loadResource(ImportIndustryResources.DECRYPTORS_CONFIG));
			decryptors = new HashMap<>();
			iterator.forEachRemaining(decryptor -> {
				decryptors.put(decryptor.getTypeId(), decryptor);
			});
		} finally {
			if (iterator != null) {
				iterator.close();
			}
		}
	}

	public Decryptor getDecryptor(long typeId) {
		return decryptors.get(typeId);
	}
}
