package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.EveConstants.DECRYPTORS_MARKET_GROUP_ID;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.Decryptor;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.common.collect.Ordering;
import java.io.File;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Loads and stores various industry-related constants into CSV files.
 */
@Log4j2
public class ImportIndustryResources implements Command {
	private static final String RESOURCES_PATH = "src/main/resources";
	private static final String RESOURCES_BASE = "/industry";
	public static final String DECRYPTORS_CONFIG = RESOURCES_BASE + "/decryptors.csv";

	@Inject
	protected CsvMapper csvMapper;

	@Inject
	protected RefDataUtil refDataUtil;

	private LoadedRefData refData;

	private DogmaAttribute inventionPropabilityMultiplier;
	private DogmaAttribute inventionMEModifier;
	private DogmaAttribute inventionTEModifier;
	private DogmaAttribute inventionMaxRunModifier;

	@Inject
	protected ImportIndustryResources() {}

	public void run() {
		if (!new File(RESOURCES_PATH + RESOURCES_BASE).exists()) {
			throw new RuntimeException("Industry resources directory does not exist");
		}
		log.info("Loading reference data");
		refData = refDataUtil.loadLatestRefData().blockingGet();
		loadDogma();
		loadDecryptors();
	}

	private void loadDogma() {
		inventionPropabilityMultiplier =
				refData.getDogmaAttribute("inventionPropabilityMultiplier").orElseThrow();
		inventionMEModifier = refData.getDogmaAttribute("inventionMEModifier").orElseThrow();
		inventionTEModifier = refData.getDogmaAttribute("inventionTEModifier").orElseThrow();
		inventionMaxRunModifier =
				refData.getDogmaAttribute("inventionMaxRunModifier").orElseThrow();
	}

	@SneakyThrows
	private void loadDecryptors() {
		log.info("Loading Decryptors");
		var decryptorTypes = refData.getAllTypes()
				.map(p -> p.getRight())
				.filter(t -> t.getMarketGroupId() != null)
				.filter(t -> t.getMarketGroupId().intValue() == DECRYPTORS_MARKET_GROUP_ID)
				.sorted(Ordering.natural().onResultOf(t -> t.getTypeId()))
				.toList();
		log.info(
				"Found {} decryptors: {}",
				decryptorTypes.size(),
				decryptorTypes.stream().map(t -> t.getTypeId()).toList());
		var file = new File(RESOURCES_PATH + DECRYPTORS_CONFIG);
		log.info("Writing decryptors to {}", file);
		var schema = csvMapper
				.schemaFor(Decryptor.class)
				.withHeader()
				.withStrictHeaders(true)
				.withColumnReordering(true);
		try (var writer = csvMapper.writer(schema).writeValues(file)) {
			//			writer.write(List.of(
			//					"typeId",
			//					"name",
			//					"inventionPropabilityMultiplier",
			//					"inventionMEModifier",
			//					"inventionTEModifier",
			//					"inventionMaxRunModifier"));
			for (var decryptorType : decryptorTypes) {
				var decryptor = Decryptor.builder()
						.typeId(decryptorType.getTypeId())
						.name(decryptorType.getName().get("en"))
						.probabilityModifier(refDataUtil
								.getTypeDogmaValue(decryptorType, inventionPropabilityMultiplier.getAttributeId())
								.orElseThrow())
						.meModifier((int) refDataUtil
								.getTypeDogmaValue(decryptorType, inventionMEModifier.getAttributeId())
								.orElseThrow())
						.teModifier((int) refDataUtil
								.getTypeDogmaValue(decryptorType, inventionTEModifier.getAttributeId())
								.orElseThrow())
						.runModifier((int) refDataUtil
								.getTypeDogmaValue(decryptorType, inventionMaxRunModifier.getAttributeId())
								.orElseThrow())
						.build();
				writer.write(decryptor);
			}
		}
	}
}
