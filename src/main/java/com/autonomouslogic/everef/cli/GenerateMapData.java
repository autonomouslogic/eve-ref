package com.autonomouslogic.everef.cli;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.refdata.Coordinate;
import com.autonomouslogic.everef.refdata.Region;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.json.JsonMapper;

/**
 * Generates a single JSON file describing all regions and their stargate connections, for use by a
 * client-side map of EVE's universe.
 */
@Log4j2
public class GenerateMapData implements Command {
	public static final String MAP_REGIONS_FILE = "map-regions.json";

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected S3Util s3Util;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	@Named("static")
	protected S3AsyncClient s3Client;

	private S3Url staticUrl;

	private final Duration cacheControlMaxAge = Configs.STATIC_CACHE_CONTROL_MAX_AGE.getRequired();

	@Inject
	protected GenerateMapData() {}

	@Inject
	protected void init() {
		staticUrl = (S3Url) urlParser.parse(Configs.STATIC_PATH.getRequired());
	}

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
			uploadFile(jsonMapper.writeValueAsBytes(regions));
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
	private void uploadFile(byte[] contents) {
		var path = staticUrl.resolve(MAP_REGIONS_FILE);
		var put = s3Util.putObjectRequest(contents.length, path, "application/json", cacheControlMaxAge);
		log.debug(String.format("Uploading %s to %s", MAP_REGIONS_FILE, path));
		s3Adapter.putObject(put, contents, s3Client);
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
