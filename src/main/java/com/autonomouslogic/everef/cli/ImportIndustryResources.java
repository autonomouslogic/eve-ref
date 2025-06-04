package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.EveConstants.DATACORES_MARKET_GROUP_ID;
import static com.autonomouslogic.everef.util.EveConstants.DECRYPTORS_MARKET_GROUP_ID;
import static com.autonomouslogic.everef.util.EveConstants.STRUCTURE_CATEGORY_ID;
import static com.autonomouslogic.everef.util.EveConstants.STRUCTURE_MODULE_CATEGORY_ID;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustrySkill;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.IndustryModifierActivities;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.common.collect.Ordering;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Loads and stores various industry-related constants into CSV files.
 */
@Log4j2
public class ImportIndustryResources implements Command {
	private static final String RESOURCES_PATH = "src/main/resources";
	private static final String RESOURCES_BASE = "/industry";
	public static final String DECRYPTORS_CONFIG = RESOURCES_BASE + "/decryptors.csv";
	public static final String STRUCTURES_CONFIG = RESOURCES_BASE + "/structures.csv";
	public static final String RIGS_CONFIG = RESOURCES_BASE + "/rigs.csv";
	public static final String SKILLS_CONFIG = RESOURCES_BASE + "/skills.csv";

	private final String ENCRYPTION_METHODS = "Encryption Methods";
	private final String DATACORE_PREFIX = "Datacore - ";
	private static final Map<String, String> DATACORE_SKILL_RENAMES = Map.ofEntries(
		Map.entry("Amarrian Starship Engineering", "Amarr Starship Engineering"),
		Map.entry("Gallentean Starship Engineering", "Gallente Starship Engineering"),
			Map.entry("Core Subsystems Engineering", "Core Subsystem Technology"),
				Map.entry("Defensive Subsystems Engineering", "Defensive Subsystem Technology"),
					Map.entry("Offensive Subsystems Engineering", "Offensive Subsystem Technology"),
						Map.entry("Propulsion Subsystems Engineering", "Propulsion Subsystem Technology")
	);

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

	private DogmaAttribute attributeEngRigTimeBonus;
	private DogmaAttribute RefRigTimeBonus;
	private DogmaAttribute attributeEngRigMatBonus;
	private DogmaAttribute attributeEngRigCostBonus;
	private DogmaAttribute RefRigMatBonus;
	private DogmaAttribute attributeThukkerEngRigMatBonus;
	private DogmaAttribute hiSecModifier;
	private DogmaAttribute lowSecModifier;
	private DogmaAttribute nullSecModifier;

	private DogmaAttribute manufacturingTimeBonus;
	private DogmaAttribute blueprintmanufactureTimeBonus;
	private DogmaAttribute copySpeedBonus;
	private DogmaAttribute advancedIndustrySkillIndustryJobTimeBonus;
	private DogmaAttribute mineralNeedResearchBonus;
	private DogmaAttribute manufactureTimePerLevel;

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
		loadRigs();
		loadSkills();
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
		RefRigMatBonus = refData.getDogmaAttribute("RefRigMatBonus").orElseThrow();
		attributeThukkerEngRigMatBonus =
				refData.getDogmaAttribute("attributeThukkerEngRigMatBonus").orElseThrow();

		attributeEngRigTimeBonus =
				refData.getDogmaAttribute("attributeEngRigTimeBonus").orElseThrow();
		RefRigTimeBonus = refData.getDogmaAttribute("RefRigTimeBonus").orElseThrow();
		attributeEngRigMatBonus =
				refData.getDogmaAttribute("attributeEngRigMatBonus").orElseThrow();
		attributeEngRigCostBonus =
				refData.getDogmaAttribute("attributeEngRigCostBonus").orElseThrow();
		hiSecModifier = refData.getDogmaAttribute("hiSecModifier").orElseThrow();
		lowSecModifier = refData.getDogmaAttribute("lowSecModifier").orElseThrow();
		nullSecModifier = refData.getDogmaAttribute("nullSecModifier").orElseThrow();

		manufacturingTimeBonus =
				refData.getDogmaAttribute("manufacturingTimeBonus").orElseThrow();
		blueprintmanufactureTimeBonus =
				refData.getDogmaAttribute("blueprintmanufactureTimeBonus").orElseThrow();
		copySpeedBonus = refData.getDogmaAttribute("copySpeedBonus").orElseThrow();
		advancedIndustrySkillIndustryJobTimeBonus = refData.getDogmaAttribute(
						"advancedIndustrySkillIndustryJobTimeBonus")
				.orElseThrow();
		mineralNeedResearchBonus =
				refData.getDogmaAttribute("mineralNeedResearchBonus").orElseThrow();
		manufactureTimePerLevel =
				refData.getDogmaAttribute("manufactureTimePerLevel").orElseThrow();
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
	private void loadRigs() {
		loadTypes("rigs", RIGS_CONFIG, IndustryRig.class, this::filterRig, this::createRig);
	}

	@SneakyThrows
	private void loadSkills() {
		var datacoreNames = getDatacoreSkillNames();
		loadTypes("skills", SKILLS_CONFIG, IndustrySkill.class, type -> filterSkill(type, datacoreNames), type -> createSkill(type, datacoreNames));
	}

	private List<String> getDatacoreSkillNames() {
		var datacoreNames = refData.getMarketGroup(DATACORES_MARKET_GROUP_ID).getTypeIds().stream()
			.map(typeId -> refData.getType(typeId))
			.flatMap(type -> Optional.ofNullable(type.getName()).flatMap(c -> Optional.ofNullable(c.get("en"))).stream())
			.map(s -> StringUtils.removeStart(s, DATACORE_PREFIX))
			.map(s -> DATACORE_SKILL_RENAMES.getOrDefault(s, s))
				.toList();
		log.info(datacoreNames);
		return datacoreNames;
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

	private boolean filterRig(InventoryType type) {
		if (type.getCategoryId() == null || type.getCategoryId().intValue() != STRUCTURE_MODULE_CATEGORY_ID) {
			return false;
		}
		return refDataUtil
				.getTypeDogmaFirstValue(
						type,
						attributeEngRigTimeBonus.getAttributeId(),
						RefRigTimeBonus.getAttributeId(),
						attributeEngRigMatBonus.getAttributeId(),
						attributeEngRigCostBonus.getAttributeId(),
						RefRigMatBonus.getAttributeId(),
						attributeThukkerEngRigMatBonus.getAttributeId())
				.isPresent();
	}

	private IndustryRig createRig(InventoryType type) {
		var builder = IndustryRig.builder()
				.typeId(type.getTypeId())
				.name(type.getName().get("en"))
				.timeBonus(refDataUtil
								.getTypeDogmaFirstValue(
										type,
										attributeEngRigTimeBonus.getAttributeId(),
										RefRigTimeBonus.getAttributeId())
								.orElse(0.0)
						/ 100.0)
				.materialBonus(refDataUtil
								.getTypeDogmaFirstValue(
										type, attributeEngRigMatBonus.getAttributeId(), RefRigMatBonus.getAttributeId())
								.orElse(0.0)
						/ 100.0)
				.thukkerMaterialBonus(refDataUtil
								.getTypeDogmaValue(type, attributeThukkerEngRigMatBonus.getAttributeId())
								.orElse(0.0)
						/ 100.0)
				.costBonus(refDataUtil
								.getTypeDogmaValue(type, attributeEngRigCostBonus.getAttributeId())
								.orElse(0.0)
						/ 100.0)
				.highSecMod(refDataUtil
						.getTypeDogmaValue(type, hiSecModifier.getAttributeId())
						.orElse(1.0))
				.lowSecMod(refDataUtil
						.getTypeDogmaValue(type, lowSecModifier.getAttributeId())
						.orElse(1.0))
				.nullSecMod(refDataUtil
						.getTypeDogmaValue(type, nullSecModifier.getAttributeId())
						.orElse(1.0));
		handleRigCategories(type.getEngineeringRigAffectedCategoryIds(), builder);
		handleRigGroups(type.getEngineeringRigAffectedGroupIds(), builder);
		handleRigGlobal(type.getEngineeringRigGlobalActivities(), builder);

		return builder.build();
	}

	private void handleRigCategories(IndustryModifierActivities categoryIds, IndustryRig.Builder builder) {
		if (categoryIds == null) {
			return;
		}
		Optional.ofNullable(categoryIds.getManufacturing()).ifPresent(builder::manufacturingCategories);
		//		Optional.ofNullable(categoryIds.getInvention()).ifPresent(builder::inventionCategories);
		Optional.ofNullable(categoryIds.getReaction()).ifPresent(builder::reactionCategories);

		if (categoryIds.getInvention() != null && !categoryIds.getInvention().isEmpty()) {
			throw new RuntimeException("invention categories supported");
		}
	}

	private void handleRigGroups(IndustryModifierActivities groupIds, IndustryRig.Builder builder) {
		if (groupIds == null) {
			return;
		}
		Optional.ofNullable(groupIds.getManufacturing()).ifPresent(builder::manufacturingGroups);
		//		Optional.ofNullable(groupIds.getInvention()).ifPresent(builder::inventionGroups);
		Optional.ofNullable(groupIds.getReaction()).ifPresent(builder::reactionGroups);

		if (groupIds.getInvention() != null && !groupIds.getInvention().isEmpty()) {
			throw new RuntimeException("invention groups supported");
		}
	}

	private void handleRigGlobal(List<String> activities, IndustryRig.Builder builder) {
		if (activities == null) {
			return;
		}
		builder.globalActivities(activities);
	}

	private boolean filterSkill(InventoryType type, List<String> datacoreNames) {
		if (!Optional.ofNullable(type.getSkill()).orElse(false)) {
			return false;
		}
		var name = type.getName().get("en");
		return name.endsWith(ENCRYPTION_METHODS) ||
			datacoreNames.contains(name)
				|| refDataUtil
						.getTypeDogmaFirstValue(
								type,
								manufacturingTimeBonus.getAttributeId(),
								blueprintmanufactureTimeBonus.getAttributeId(),
								copySpeedBonus.getAttributeId(),
								advancedIndustrySkillIndustryJobTimeBonus.getAttributeId(),
								mineralNeedResearchBonus.getAttributeId(),
								manufactureTimePerLevel.getAttributeId())
						.isPresent();
	}

	private IndustrySkill createSkill(InventoryType type, List<String> datacoreNames) {
		var name = type.getName().get("en");
		return IndustrySkill.builder()
				.typeId(type.getTypeId())
				.name(name)
				.manufacturingTimeBonus(refDataUtil
						.getTypeDogmaValueBoxed(type, manufacturingTimeBonus.getAttributeId())
						.orElse(null))
				.blueprintmanufactureTimeBonus(refDataUtil
						.getTypeDogmaValueBoxed(type, blueprintmanufactureTimeBonus.getAttributeId())
						.orElse(null))
				.copySpeedBonus(refDataUtil
						.getTypeDogmaValueBoxed(type, copySpeedBonus.getAttributeId())
						.orElse(null))
				.advancedIndustrySkillIndustryJobTimeBonus(refDataUtil
						.getTypeDogmaValueBoxed(type, advancedIndustrySkillIndustryJobTimeBonus.getAttributeId())
						.orElse(null))
				.mineralNeedResearchBonus(refDataUtil
						.getTypeDogmaValueBoxed(type, mineralNeedResearchBonus.getAttributeId())
						.orElse(null))
				.manufactureTimePerLevel(refDataUtil
						.getTypeDogmaValueBoxed(type, manufactureTimePerLevel.getAttributeId())
						.orElse(null))
				.datacore(datacoreNames.contains(name))
				.encryptionMethods(name.endsWith(ENCRYPTION_METHODS))
				.build();
	}
}
