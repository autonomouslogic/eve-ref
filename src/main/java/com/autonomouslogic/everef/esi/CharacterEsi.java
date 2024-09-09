package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.apis.CharacterApi;
import com.autonomouslogic.everef.openapi.esi.apis.CorporationApi;
import com.autonomouslogic.everef.openapi.esi.models.GetCharactersCharacterIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetCorporationsCorporationIdOk;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Log4j2
public class CharacterEsi {
	@Inject
	protected CharacterApi characterApi;

	private final String datasource = Configs.ESI_DATASOURCE.getRequired();

	private final Map<Integer, Optional<GetCharactersCharacterIdOk>> characters = new ConcurrentHashMap<>();

	@Inject
	protected CharacterEsi() {}

	public Maybe<GetCharactersCharacterIdOk> getCharacter(int characterId) {
		return EsiHelper.getFromCacheOrFetch(
				"character", GetCharactersCharacterIdOk.class, characters, characterId, () -> {
					var source = CharacterApi.DatasourceGetCharactersCharacterId.valueOf(datasource);
					return characterApi.getCharactersCharacterId(characterId, source, null);
				});
	}
}
