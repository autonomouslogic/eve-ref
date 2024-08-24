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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;

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

	// === gets

	@SneakyThrows
	public InventoryCategory getCategory(long id) {
		return Optional.ofNullable(categories.get(id))
				.map(bytes -> parse(bytes, InventoryCategory.class))
				.orElse(null);
	}

	@SneakyThrows
	public InventoryGroup getGroup(long id) {
		return Optional.ofNullable(groups.get(id))
				.map(bytes -> parse(bytes, InventoryGroup.class))
				.orElse(null);
	}

	@SneakyThrows
	public MarketGroup getMarketGroup(long id) {
		return Optional.ofNullable(marketGroups.get(id))
				.map(bytes -> parse(bytes, MarketGroup.class))
				.orElse(null);
	}

	@SneakyThrows
	public InventoryType getType(long id) {
		return Optional.ofNullable(types.get(id))
				.map(bytes -> parse(bytes, InventoryType.class))
				.orElse(null);
	}

	@SneakyThrows
	public DogmaAttribute getDogmaAttribute(long id) {
		return Optional.ofNullable(dogmaAttributes.get(id))
				.map(bytes -> parse(bytes, DogmaAttribute.class))
				.orElse(null);
	}

	@SneakyThrows
	public DogmaEffect getDogmaEffect(long id) {
		return Optional.ofNullable(dogmaEffects.get(id))
				.map(bytes -> parse(bytes, DogmaEffect.class))
				.orElse(null);
	}

	@SneakyThrows
	public MetaGroup getMetaGroup(long id) {
		return Optional.ofNullable(metaGroups.get(id))
				.map(bytes -> parse(bytes, MetaGroup.class))
				.orElse(null);
	}

	@SneakyThrows
	public Mutaplasmid getMutaplasmid(long id) {
		return Optional.ofNullable(mutaplasmids.get(id))
				.map(bytes -> parse(bytes, Mutaplasmid.class))
				.orElse(null);
	}

	@SneakyThrows
	public Skill getSkill(long id) {
		return Optional.ofNullable(skills.get(id))
				.map(bytes -> parse(bytes, Skill.class))
				.orElse(null);
	}

	@SneakyThrows
	public Unit getUnit(long id) {
		return Optional.ofNullable(units.get(id))
				.map(bytes -> parse(bytes, Unit.class))
				.orElse(null);
	}

	@SneakyThrows
	public Blueprint getBlueprint(long id) {
		return Optional.ofNullable(blueprints.get(id))
				.map(bytes -> parse(bytes, Blueprint.class))
				.orElse(null);
	}

	@SneakyThrows
	public Icon getIcon(long id) {
		return Optional.ofNullable(icons.get(id))
				.map(bytes -> parse(bytes, Icon.class))
				.orElse(null);
	}

	@SneakyThrows
	public Region getRegion(long id) {
		return Optional.ofNullable(regions.get(id))
				.map(bytes -> parse(bytes, Region.class))
				.orElse(null);
	}

	@SneakyThrows
	public Schematic getSchematic(long id) {
		return Optional.ofNullable(schematics.get(id))
				.map(bytes -> parse(bytes, Schematic.class))
				.orElse(null);
	}

	// === puts

	@SneakyThrows
	public void putCategory(long id, InventoryCategory item) {
		categories.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putGroup(long id, InventoryGroup item) {
		groups.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putMarketGroup(long id, MarketGroup item) {
		marketGroups.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putType(long id, InventoryType item) {
		types.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putDogmaAttribute(long id, DogmaAttribute item) {
		dogmaAttributes.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putDogmaEffect(long id, DogmaEffect item) {
		dogmaEffects.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putMetaGroup(long id, MetaGroup item) {
		metaGroups.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putMutaplasmid(long id, Mutaplasmid item) {
		mutaplasmids.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putSkill(long id, Skill item) {
		skills.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putUnit(long id, Unit item) {
		units.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putBlueprint(long id, Blueprint item) {
		blueprints.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putIcon(long id, Icon item) {
		icons.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putRegion(long id, Region item) {
		regions.put(id, objectMapper.writeValueAsBytes(item));
	}

	@SneakyThrows
	public void putSchematic(long id, Schematic item) {
		schematics.put(id, objectMapper.writeValueAsBytes(item));
	}

	// === streams

	public Stream<Pair<Long, InventoryCategory>> getAllCategories() {
		return categories.keySet().stream().map(id -> Pair.of(id, getCategory(id)));
	}

	public Stream<Pair<Long, InventoryGroup>> getAllGroups() {
		return groups.keySet().stream().map(id -> Pair.of(id, getGroup(id)));
	}

	public Stream<Pair<Long, MarketGroup>> getAllMarketGroups() {
		return marketGroups.keySet().stream().map(id -> Pair.of(id, getMarketGroup(id)));
	}

	public Stream<Pair<Long, InventoryType>> getAllTypes() {
		return types.keySet().stream().map(id -> Pair.of(id, getType(id)));
	}

	public Stream<Pair<Long, DogmaAttribute>> getAllDogmaAttributes() {
		return dogmaAttributes.keySet().stream().map(id -> Pair.of(id, getDogmaAttribute(id)));
	}

	public Stream<Pair<Long, DogmaEffect>> getAllDogmaEffects() {
		return dogmaEffects.keySet().stream().map(id -> Pair.of(id, getDogmaEffect(id)));
	}

	public Stream<Pair<Long, MetaGroup>> getAllMetaGroups() {
		return metaGroups.keySet().stream().map(id -> Pair.of(id, getMetaGroup(id)));
	}

	public Stream<Pair<Long, Mutaplasmid>> getAllMutaplasmids() {
		return mutaplasmids.keySet().stream().map(id -> Pair.of(id, getMutaplasmid(id)));
	}

	public Stream<Pair<Long, Skill>> getAllSkills() {
		return skills.keySet().stream().map(id -> Pair.of(id, getSkill(id)));
	}

	public Stream<Pair<Long, Unit>> getAllUnits() {
		return units.keySet().stream().map(id -> Pair.of(id, getUnit(id)));
	}

	public Stream<Pair<Long, Blueprint>> getAllBlueprints() {
		return blueprints.keySet().stream().map(id -> Pair.of(id, getBlueprint(id)));
	}

	public Stream<Pair<Long, Icon>> getAllIcons() {
		return icons.keySet().stream().map(id -> Pair.of(id, getIcon(id)));
	}

	public Stream<Pair<Long, Region>> getAllRegions() {
		return regions.keySet().stream().map(id -> Pair.of(id, getRegion(id)));
	}

	public Stream<Pair<Long, Schematic>> getAllSchematics() {
		return schematics.keySet().stream().map(id -> Pair.of(id, getSchematic(id)));
	}

	// === util

	@SneakyThrows
	private <T> T parse(byte[] bytes, Class<T> clazz) {
		return objectMapper.readValue(bytes, clazz);
	}
}
