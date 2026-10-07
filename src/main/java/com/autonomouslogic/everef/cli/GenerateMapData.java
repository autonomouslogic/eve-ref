package com.autonomouslogic.everef.cli;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.refdata.Coordinate;
import com.autonomouslogic.everef.refdata.Region;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.json.JsonMapper;

/**
 * Generates a single JSON file describing all regions and their stargate connections, for use by a
 * client-side map of EVE's universe. Written directly into the UI repo so it can be imported there.
 */
@Log4j2
public class GenerateMapData implements Command {
	public static final String MAP_REGIONS_FILE = "ui/assets/data/map-regions.json";

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected GenerateMapData() {}

	@Override
	@SneakyThrows
	public void run() {
		VirtualThreads.checkIsVirtual();
		var refData = refDataUtil.loadLatestRefData().blockingGet();
		try {
			var regions = refData.getAllRegions()
					.map(pair -> toMapRegion(pair.getRight()))
					.sorted(Comparator.comparing(MapRegion::getRegionId))
					.toList();
			log.info("Loaded {} regions", regions.size());
			writeFile(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(regions));
		} finally {
			refData.close();
		}
	}

	private static MapRegion toMapRegion(Region region) {
		return MapRegion.builder()
				.regionId(region.getRegionId())
				.universeId(region.getUniverseId())
				.name(region.getName() == null ? null : region.getName().get("en"))
				.position(region.getPosition())
				.stargatesToRegionIds(region.getStargatesToRegionIds())
				.build();
	}

	@SneakyThrows
	private void writeFile(byte[] contents) {
		var file = new File(MAP_REGIONS_FILE);
		log.debug("Writing {}", file);
		FileUtils.writeByteArrayToFile(file, contents);
	}

	@Value
	@Builder
	@Jacksonized
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public static class MapRegion {
		@JsonProperty
		Long regionId;

		@JsonProperty
		String universeId;

		@JsonProperty
		String name;

		@JsonProperty
		Coordinate position;

		@JsonProperty
		List<Long> stargatesToRegionIds;
	}
}
