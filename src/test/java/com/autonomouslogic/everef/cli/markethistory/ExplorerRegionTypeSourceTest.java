package com.autonomouslogic.everef.cli.markethistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Region;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_MARKET_HISTORY_EXPLORATION_GROUPS", value = "50")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/data")
@Log4j2
public class ExplorerRegionTypeSourceTest {
	@Inject
	ExplorerRegionTypeSource source;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	ObjectMapper objectMapper;

	MockWebServer server;

	LocalDate today = LocalDate.parse("2023-01-01");
	int groups = Configs.ESI_MARKET_HISTORY_EXPLORATION_GROUPS.getRequired();
	Map<Long, InventoryType> allTypes;
	Map<Long, Region> allRegions;
	List<RegionTypePair> validPairs;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

		source.setToday(today);

		allTypes = new HashMap<>();
		allRegions = new HashMap<>();
		var t = 10000;
		var r = 20000;
		for (int i = 0; i < 200; i++) {
			var type = InventoryType.builder()
					.typeId((long) t++)
					.marketGroupId(100L)
					.build();
			allTypes.put(type.getTypeId(), type);
		}
		for (int i = 0; i < 100; i++) {
			var type = InventoryType.builder()
					.typeId((long) t++)
					.marketGroupId(null)
					.build();
			allTypes.put(type.getTypeId(), type);
		}
		for (var universeId : List.of("eve", "wormhole", "void", "abyssal")) {
			for (int i = 0; i < 10; i++) {
				var region = Region.builder()
						.regionId((long) r++)
						.universeId(universeId)
						.build();
				allRegions.put(region.getRegionId(), region);
			}
		}

		validPairs = allRegions.values().stream()
				.filter(region -> List.of("eve", "wormhole").contains(region.getUniverseId()))
				.flatMap(region -> allTypes.values().stream()
						.filter(type -> type.getMarketGroupId() != null)
						.map(type -> new RegionTypePair(
								region.getRegionId().intValue(),
								type.getTypeId().intValue())))
				.toList();
		assertEquals(200 * (2 * 10), validPairs.size());

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@Test
	void shouldSourcePairs() {
		var pairs = source.sourcePairs(List.of()).toList().blockingGet();
		assertNotEquals(0, pairs.size());
		for (var pair : pairs) {
			assertTrue(validPairs.contains(pair), pair.toString());
		}
	}

	@Test
	void shouldSourceRoughlyTheSameNumberOfPairsEachDay() {
		var summary = new IntSummaryStatistics();
		for (int i = 0; i < groups; i++) {
			var pairs = source.setToday(today.plusDays(i))
					.sourcePairs(List.of())
					.toList()
					.blockingGet();
			summary.accept(pairs.size());
		}
		log.info(summary.toString());
		assertEquals((double) validPairs.size() / groups, summary.getAverage(), Double.MIN_VALUE);
		assertNotEquals(0, summary.getMin());
		assertEquals(validPairs.size(), summary.getSum());
	}

	@Test
	void shouldSourceAllPairsWhenGoingOverAllGroups() {
		var seen = new HashSet<RegionTypePair>();
		for (int i = 0; i < groups; i++) {
			var pairs = source.setToday(today.plusDays(i))
					.sourcePairs(List.of())
					.toList()
					.blockingGet();
			for (var pair : pairs) {
				assertFalse(seen.contains(pair), pair.toString());
				assertTrue(validPairs.contains(pair), pair.toString());
			}
			seen.addAll(pairs);
		}
		assertEquals(validPairs.size(), seen.size());
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var refdataPath = "/data/" + ArchivePathFactory.REFERENCE_DATA.createLatestPath();
				if (path.equals(refdataPath)) {
					var compressed = new ByteArrayOutputStream();
					try (var out = new TarArchiveOutputStream(new XZCompressorOutputStream(compressed))) {
						try {
							var regionsJson = objectMapper.writeValueAsBytes(allRegions);
							var typesJson = objectMapper.writeValueAsBytes(allTypes);
							var regionsEntry = new TarArchiveEntry("regions.json");
							var typesEntry = new TarArchiveEntry("types.json");
							regionsEntry.setSize(regionsJson.length);
							typesEntry.setSize(typesJson.length);
							out.putArchiveEntry(regionsEntry);
							out.write(regionsJson);
							out.closeArchiveEntry();
							out.putArchiveEntry(typesEntry);
							out.write(typesJson);
							out.closeArchiveEntry();
						} catch (Exception e) {
							log.error("Error writing", e);
							throw e;
						}
					}
					return testDataUtil.mockResponse(compressed);
				}
				log.error(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(500);
			} catch (Exception e) {
				fail("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
