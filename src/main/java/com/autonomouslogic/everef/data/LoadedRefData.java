package com.autonomouslogic.everef.data;

import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.DogmaEffect;
import com.autonomouslogic.everef.refdata.Icon;
import com.autonomouslogic.everef.refdata.InventoryCategory;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.refdata.MetaGroup;
import com.autonomouslogic.everef.refdata.Mutaplasmid;
import com.autonomouslogic.everef.refdata.Region;
import com.autonomouslogic.everef.refdata.Schematic;
import com.autonomouslogic.everef.refdata.Skill;
import com.autonomouslogic.everef.refdata.Unit;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;

@Log4j2
public class LoadedRefData {
	@Inject
	protected ObjectMapper objectMapper;

	private final MVStore mvStore;

	private final Map<Long, byte[]> categories;
	private final Map<Long, byte[]> groups;
	private final Map<Long, byte[]> marketGroups;
	private final Map<Long, byte[]> types;
	private final Map<Long, byte[]> dogmaAttributes;
	private final Map<Long, byte[]> dogmaEffects;
	private final Map<Long, byte[]> metaGroups;
	private final Map<Long, byte[]> mutaplasmids;
	private final Map<Long, byte[]> skills;
	private final Map<Long, byte[]> units;
	private final Map<Long, byte[]> blueprints;
	private final Map<Long, byte[]> icons;
	private final Map<Long, byte[]> regions;
	private final Map<Long, byte[]> schematics;

	@Inject
	protected LoadedRefData(MVStoreUtil mvStoreUtil) {
		mvStore = mvStoreUtil.createTempStore("ref-data");
		mvStore.setCacheSize(8 * 1024 * 1024);

		categories = mvStore.openMap("categories");
		groups = mvStore.openMap("groups");
		marketGroups = mvStore.openMap("marketGroups");
		types = mvStore.openMap("types");
		dogmaAttributes = mvStore.openMap("dogmaAttributes");
		dogmaEffects = mvStore.openMap("dogmaEffects");
		metaGroups = mvStore.openMap("metaGroups");
		mutaplasmids = mvStore.openMap("mutaplasmids");
		skills = mvStore.openMap("skills");
		units = mvStore.openMap("units");
		blueprints = mvStore.openMap("blueprints");
		icons = mvStore.openMap("icons");
		regions = mvStore.openMap("regions");
		schematics = mvStore.openMap("schematics");
	}

	// === wrapper

	private <T> T get(long id, Map<Long, byte[]> map, Class<T> clazz) {
		return Optional.ofNullable(map.get(id))
				.map(bytes -> parse(bytes, clazz))
				.orElse(null);
	}

	@SneakyThrows
	private void put(long id, Object item, Map<Long, byte[]> map) {
		map.put(id, objectMapper.writeValueAsBytes(item));
	}

	private <T> Stream<Pair<Long, T>> stream(Map<Long, byte[]> map, Function<Long, T> getter) {
		return map.keySet().stream().map(id -> Pair.of(id, getter.apply(id)));
	}

	// === gets

	public InventoryCategory getCategory(long id) {
		return get(id, categories, InventoryCategory.class);
	}

	public InventoryGroup getGroup(long id) {
		return get(id, groups, InventoryGroup.class);
	}

	public MarketGroup getMarketGroup(long id) {
		return get(id, marketGroups, MarketGroup.class);
	}

	public InventoryType getType(long id) {
		return get(id, types, InventoryType.class);
	}

	public DogmaAttribute getDogmaAttribute(long id) {
		return get(id, dogmaAttributes, DogmaAttribute.class);
	}

	public DogmaEffect getDogmaEffect(long id) {
		return get(id, dogmaEffects, DogmaEffect.class);
	}

	public MetaGroup getMetaGroup(long id) {
		return get(id, metaGroups, MetaGroup.class);
	}

	public Mutaplasmid getMutaplasmid(long id) {
		return get(id, mutaplasmids, Mutaplasmid.class);
	}

	public Skill getSkill(long id) {
		return get(id, skills, Skill.class);
	}

	public Unit getUnit(long id) {
		return get(id, units, Unit.class);
	}

	public Blueprint getBlueprint(long id) {
		return get(id, blueprints, Blueprint.class);
	}

	public Icon getIcon(long id) {
		return get(id, icons, Icon.class);
	}

	public Region getRegion(long id) {
		return get(id, regions, Region.class);
	}

	public Schematic getSchematic(long id) {
		return get(id, schematics, Schematic.class);
	}

	// === puts

	public void putCategory(long id, InventoryCategory item) {
		put(id, item, categories);
	}

	public void putGroup(long id, InventoryGroup item) {
		put(id, item, groups);
	}

	public void putMarketGroup(long id, MarketGroup item) {
		put(id, item, marketGroups);
	}

	public void putType(long id, InventoryType item) {
		put(id, item, types);
	}

	public void putDogmaAttribute(long id, DogmaAttribute item) {
		put(id, item, dogmaAttributes);
	}

	public void putDogmaEffect(long id, DogmaEffect item) {
		put(id, item, dogmaEffects);
	}

	public void putMetaGroup(long id, MetaGroup item) {
		put(id, item, metaGroups);
	}

	public void putMutaplasmid(long id, Mutaplasmid item) {
		put(id, item, mutaplasmids);
	}

	public void putSkill(long id, Skill item) {
		put(id, item, skills);
	}

	public void putUnit(long id, Unit item) {
		put(id, item, units);
	}

	public void putBlueprint(long id, Blueprint item) {
		put(id, item, blueprints);
	}

	public void putIcon(long id, Icon item) {
		put(id, item, icons);
	}

	public void putRegion(long id, Region item) {
		put(id, item, regions);
	}

	public void putSchematic(long id, Schematic item) {
		put(id, item, schematics);
	}

	// === streams

	public Stream<Pair<Long, InventoryCategory>> getAllCategories() {
		return stream(categories, this::getCategory);
	}

	public Stream<Pair<Long, InventoryGroup>> getAllGroups() {
		return stream(groups, this::getGroup);
	}

	public Stream<Pair<Long, MarketGroup>> getAllMarketGroups() {
		return stream(marketGroups, this::getMarketGroup);
	}

	public Stream<Pair<Long, InventoryType>> getAllTypes() {
		return stream(types, this::getType);
	}

	public Stream<Pair<Long, DogmaAttribute>> getAllDogmaAttributes() {
		return stream(dogmaAttributes, this::getDogmaAttribute);
	}

	public Stream<Pair<Long, DogmaEffect>> getAllDogmaEffects() {
		return stream(dogmaEffects, this::getDogmaEffect);
	}

	public Stream<Pair<Long, MetaGroup>> getAllMetaGroups() {
		return stream(metaGroups, this::getMetaGroup);
	}

	public Stream<Pair<Long, Mutaplasmid>> getAllMutaplasmids() {
		return stream(mutaplasmids, this::getMutaplasmid);
	}

	public Stream<Pair<Long, Skill>> getAllSkills() {
		return stream(skills, this::getSkill);
	}

	public Stream<Pair<Long, Unit>> getAllUnits() {
		return stream(units, this::getUnit);
	}

	public Stream<Pair<Long, Blueprint>> getAllBlueprints() {
		return stream(blueprints, this::getBlueprint);
	}

	public Stream<Pair<Long, Icon>> getAllIcons() {
		return stream(icons, this::getIcon);
	}

	public Stream<Pair<Long, Region>> getAllRegions() {
		return stream(regions, this::getRegion);
	}

	public Stream<Pair<Long, Schematic>> getAllSchematics() {
		return stream(schematics, this::getSchematic);
	}

	// === util

	@SneakyThrows
	private <T> T parse(byte[] bytes, Class<T> clazz) {
		return objectMapper.readValue(bytes, clazz);
	}

	public void close() {
		var filename = mvStore.getFileStore().getFileName();
		try {
			mvStore.closeImmediately();
		} catch (Exception e) {
			log.warn("Failed closing MVStore, ignoring", e);
		}
		try {
			new File(filename).delete();
		} catch (Exception e) {
			log.warn("Failed deleting MVStore file, ignoring", e);
		}
	}
}
