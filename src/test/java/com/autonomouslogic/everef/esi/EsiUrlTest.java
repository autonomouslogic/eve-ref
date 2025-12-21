package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "https://esi.evetech.net/")
public class EsiUrlTest {

	@Test
	void shouldBuildBasicUrl() {
		var url = EsiUrl.builder().urlPath("/universe/types/").build();
		assertEquals(
				"https://esi.evetech.net/latest/universe/types/?datasource=tranquility&language=en", url.toString());
	}

	@Test
	void shouldBuildUrlWithPage() {
		var url = EsiUrl.builder().urlPath("/markets/orders/").page(2).build();
		assertEquals(
				"https://esi.evetech.net/latest/markets/orders/?datasource=tranquility&language=en&page=2",
				url.toString());
	}

	@Test
	void shouldHandleUrlWithExistingQueryString() {
		var url = EsiUrl.builder().urlPath("/universe/types/?type_id=34").build();
		assertEquals(
				"https://esi.evetech.net/latest/universe/types/?type_id=34&datasource=tranquility&language=en",
				url.toString());
	}

	@Test
	void shouldBuildUrlWithCustomBasePath() {
		var url = EsiUrl.builder().urlPath("/universe/types/").basePath("v4").build();
		assertEquals("https://esi.evetech.net/v4/universe/types/?datasource=tranquility&language=en", url.toString());
	}

	@Test
	void shouldBuildUrlWithCustomDatasource() {
		var url = EsiUrl.builder()
				.urlPath("/universe/types/")
				.datasource("singularity")
				.build();
		assertEquals(
				"https://esi.evetech.net/latest/universe/types/?datasource=singularity&language=en", url.toString());
	}

	@Test
	void shouldBuildUrlWithCustomLanguage() {
		var url = EsiUrl.builder().urlPath("/universe/types/").language("de").build();
		assertEquals(
				"https://esi.evetech.net/latest/universe/types/?datasource=tranquility&language=de", url.toString());
	}

	@Test
	void shouldBuildModernUrl() {
		var url = EsiUrl.modern().urlPath("/freelance-jobs").build();
		assertEquals("https://esi.evetech.net/freelance-jobs", url.toString());
	}

	@Test
	void shouldBuildModernUrl2() {
		var url = EsiUrl.modern().urlPath("/freelance-jobs/object-id").build();
		assertEquals("https://esi.evetech.net/freelance-jobs/object-id", url.toString());
	}

	@Test
	void shouldBuildModernUrlWithAfter() {
		var url = EsiUrl.modern()
				.urlPath("/freelance-jobs/object-id")
				.after("dsdwrsf")
				.build();
		assertEquals("https://esi.evetech.net/freelance-jobs/object-id?after=dsdwrsf", url.toString());
	}

	@Test
	void shouldBuildModernUrlWithBefore() {
		var url = EsiUrl.modern()
				.urlPath("/freelance-jobs/object-id")
				.before("dsdwrsf")
				.build();
		assertEquals("https://esi.evetech.net/freelance-jobs/object-id?before=dsdwrsf", url.toString());
	}

	@Test
	void shouldThrowExceptionForInvalidPage() {
		var url = EsiUrl.builder().urlPath("/markets/orders/").page(0).build();
		var exception = assertThrows(IllegalArgumentException.class, () -> url.toString());
		assertEquals("Page must be >= 1", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionForNegativePage() {
		var url = EsiUrl.builder().urlPath("/markets/orders/").page(-1).build();
		var exception = assertThrows(IllegalArgumentException.class, () -> url.toString());
		assertEquals("Page must be >= 1", exception.getMessage());
	}

	@Test
	@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "https://esi.evetech.net")
	void shouldBuildBasicUrlWithNoTrailingSlash() {
		var url = EsiUrl.builder().urlPath("/universe/types/").build();
		assertEquals(
				"https://esi.evetech.net/latest/universe/types/?datasource=tranquility&language=en", url.toString());
	}

	@Test
	@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "https://esi.evetech.net")
	void shouldBuildModernUrlWithNoTrailingSlash() {
		var url = EsiUrl.modern().urlPath("/universe/types/").build();
		assertEquals("https://esi.evetech.net/universe/types/", url.toString());
	}
}
