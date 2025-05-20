package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.EveConstants.DECRYPTORS_MARKET_GROUP_ID;
import static com.autonomouslogic.everef.util.EveConstants.STRUCTURE_CATEGORY_ID;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.common.collect.Ordering;
import java.io.File;
import java.util.function.Function;
import java.util.function.Predicate;
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
	public static final String STRUCTURES_CONFIG = RESOURCES_BASE + "/structures.csv";

	@Inject
	protected CsvMapper csvMapper;

	@Inject
	protected RefDataUtil refDataUtil;

	private LoadedRefData refData;

	private DogmaAttribute inventionPropabilityMultiplier;
	private DogmaAttribute inventionMEModifier;
	private DogmaAttribute inventionTEModifier;
	private DogmaAttribute inventionMaxRunModifier;

	private DogmaAttribute strEngMatBonus;
	private DogmaAttribute strEngCostBonus;
	private DogmaAttribute strEngTimeBonus;

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
		loadStructures();
	}

	private void loadDogma() {
		inventionPropabilityMultiplier =
				refData.getDogmaAttribute("inventionPropabilityMultiplier").orElseThrow();
		inventionMEModifier = refData.getDogmaAttribute("inventionMEModifier").orElseThrow();
		inventionTEModifier = refData.getDogmaAttribute("inventionTEModifier").orElseThrow();
		inventionMaxRunModifier =
				refData.getDogmaAttribute("inventionMaxRunModifier").orElseThrow();

		strEngMatBonus = refData.getDogmaAttribute("strEngMatBonus").orElseThrow();
		strEngCostBonus = refData.getDogmaAttribute("strEngCostBonus").orElseThrow();
		strEngTimeBonus = refData.getDogmaAttribute("strEngTimeBonus").orElseThrow();
	}

	@SneakyThrows
	private void loadDecryptors() {
		loadTypes(
				"decryptors", DECRYPTORS_CONFIG, IndustryDecryptor.class, this::filterDecryptor, this::createDecryptor);
	}

	@SneakyThrows
	private void loadStructures() {
		loadTypes(
				"structures", STRUCTURES_CONFIG, IndustryStructure.class, this::filterStructure, this::createStructure);
	}

	@SneakyThrows
	private <T> void loadTypes(
			String name,
			String filename,
			Class<T> clazz,
			Predicate<InventoryType> filter,
			Function<InventoryType, T> factory) {
		log.info("Loading {}", name);
		var types = refData.getAllTypes()
				.map(p -> p.getRight())
				.filter(filter)
				.sorted(Ordering.natural().onResultOf(t -> t.getTypeId()))
				.toList();
		log.info(
				"Found {} {}: {}",
				types.size(),
				name,
				types.stream().map(t -> t.getTypeId()).toList());
		var file = new File(RESOURCES_PATH + filename);
		log.info("Writing {} to {}", name, file);
		var schema =
				csvMapper.schemaFor(clazz).withHeader().withStrictHeaders(true).withColumnReordering(true);
		try (var writer = csvMapper.writer(schema).writeValues(file)) {
			for (var type : types) {
				var model = factory.apply(type);
				writer.write(model);
			}
		}
		log.info("Finished writing {}", name);
	}

	private boolean filterDecryptor(InventoryType type) {

		return type.getMarketGroupId() != null && type.getMarketGroupId().intValue() == DECRYPTORS_MARKET_GROUP_ID;
	}

	private IndustryDecryptor createDecryptor(InventoryType type) {
		return IndustryDecryptor.builder()
				.typeId(type.getTypeId())
				.name(type.getName().get("en"))
				.probabilityModifier(refDataUtil
						.getTypeDogmaValue(type, inventionPropabilityMultiplier.getAttributeId())
						.orElseThrow())
				.meModifier((int) refDataUtil
						.getTypeDogmaValue(type, inventionMEModifier.getAttributeId())
						.orElseThrow())
				.teModifier((int) refDataUtil
						.getTypeDogmaValue(type, inventionTEModifier.getAttributeId())
						.orElseThrow())
				.runModifier((int) refDataUtil
						.getTypeDogmaValue(type, inventionMaxRunModifier.getAttributeId())
						.orElseThrow())
				.build();
	}

	private boolean filterStructure(InventoryType type) {
		return type.getCategoryId() != null && type.getCategoryId().intValue() == STRUCTURE_CATEGORY_ID;
	}

	private IndustryStructure createStructure(InventoryType type) {
		return IndustryStructure.builder()
				.typeId(type.getTypeId())
				.name(type.getName().get("en"))
				.materialModifier(refDataUtil
						.getTypeDogmaValue(type, strEngMatBonus.getAttributeId())
						.orElse(1.0))
				.timeModifier(refDataUtil
						.getTypeDogmaValue(type, strEngTimeBonus.getAttributeId())
						.orElse(1.0))
				.costModifier(refDataUtil
						.getTypeDogmaValue(type, strEngCostBonus.getAttributeId())
						.orElse(1.0))
				.build();
	}
}
