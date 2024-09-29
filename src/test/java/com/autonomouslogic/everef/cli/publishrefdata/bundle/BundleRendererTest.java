package com.autonomouslogic.everef.cli.publishrefdata.bundle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.InventoryTypeTraits;
import com.autonomouslogic.everef.refdata.TraitBonus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BundleRendererTest {
	ObjectMapper objectMapper = new ObjectMapper();
	TestBundleRenderer renderer;

	@BeforeEach
	void setup() {
		renderer = new TestBundleRenderer();
	}

	@ParameterizedTest
	@MethodSource("showInfoLinkTests")
	void shouldHandleShowInfoLinks(String text, List<Long> expectedTypes) {
		renderer.typesMap = Map.of(
				11396L, objectMapper.createObjectNode(),
				645L, objectMapper.createObjectNode());
		var typesJson = objectMapper.createObjectNode();
		renderer.bundleShowInfo(text, typesJson);
		for (var id : expectedTypes) {
			assertTrue(typesJson.has(Long.toString(id)), "ID: " + id);
		}
	}

	static Stream<Arguments> showInfoLinkTests() {
		return Stream.of(
				Arguments.of("Input <a href=showinfo:11396>Mercoxit</a> string", List.of(11396L)),
				Arguments.of("Input <url href=showinfo:11396>Mercoxit</url> string", List.of(11396L)),
				Arguments.of(
						"<a href=showinfo:11396>Mercoxit</a> <a href=showinfo:645>Dominix</a>", List.of(11396L, 645L)),
				Arguments.of(
						"<url href=showinfo:11396>Mercoxit</url> <url href=showinfo:645>Dominix</url>",
						List.of(11396L, 645L)));
	}

	@Test
	void shouldBundleShowinfoFromTraits() {
		renderer.typesMap = Map.of(
				11396L, objectMapper.createObjectNode(),
				645L, objectMapper.createObjectNode(),
				644L, objectMapper.createObjectNode());
		var type = InventoryType.builder()
				.traits(InventoryTypeTraits.builder()
						.miscBonuses(Map.of(
								"1",
								TraitBonus.builder()
										.bonusText(Map.of("en", "Input <a href=showinfo:11396>Mercoxit</a> string"))
										.build()))
						.roleBonuses(Map.of(
								"1",
								TraitBonus.builder()
										.bonusText(Map.of("en", "Input <a href=showinfo:645>Dominix</a> string"))
										.build()))
						.types(Map.of(
								"100",
								Map.of(
										"1",
										TraitBonus.builder()
												.bonusText(
														Map.of("en", "Input <a href=showinfo:644>Typhoon</a> string"))
												.build())))
						.build())
				.build();
		var typesJson = objectMapper.createObjectNode();
		renderer.bundleTraits(type, typesJson);
		assertTrue(typesJson.has("11396"));
		assertTrue(typesJson.has("645"));
		assertTrue(typesJson.has("644"));
	}

	static class TestBundleRenderer extends BundleRenderer {
		@Override
		protected Flowable<Pair<String, JsonNode>> renderInternal() {
			return Flowable.empty();
		}
	}
}
