package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Log4j2
public class EsiVerifyResponseTest {
	@Inject
	ObjectMapper objectMapper;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	@SneakyThrows
	public void shouldDeserialise() {
		var json = objectMapper
				.createObjectNode()
				.put("CharacterID", 2113778331)
				.put("CharacterName", "EVE Ref")
				.put("ExpiresOn", "2024-03-21T04:18:03")
				.put("Scopes", "esi-markets.structure_markets.v1 esi-universe.read_structures.v1")
				.put("CharacterOwnerHash", "random-hash")
				.toString();
		var parsed = objectMapper.readValue(json, EsiVerifyResponse.class);
		log.info(json);
		log.info(parsed);
		assertEquals(2113778331, parsed.getCharacterId());
		assertEquals("EVE Ref", parsed.getCharacterName());
		assertEquals(Instant.parse("2024-03-21T04:18:03Z"), parsed.getExpiresOn());
		assertEquals(
				List.of("esi-markets.structure_markets.v1", "esi-universe.read_structures.v1"), parsed.getScopes());
		assertEquals("random-hash", parsed.getCharacterOwnerHash());
	}
}
