package com.autonomouslogic.everef.esi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class EsiVerifyResponse {
	@JsonProperty("CharacterID")
	long characterId;

	@JsonProperty
	String characterName;

	@JsonProperty
	String characterOwnerHash;

	@JsonProperty
	@JsonDeserialize(using = ExpiresOnDeserializer.class)
	Instant expiresOn;

	@JsonProperty
	@JsonDeserialize(using = ScopesDeserializer.class)
	List<String> scopes;

	public static final class ExpiresOnDeserializer extends ValueDeserializer<Instant> {
		@Override
		public Instant deserialize(JsonParser p, DeserializationContext ctxt) {
			return LocalDateTime.parse(p.getText()).atZone(ZoneOffset.UTC).toInstant();
		}
	}

	public static final class ScopesDeserializer extends ValueDeserializer<List<String>> {
		@Override
		public List<String> deserialize(JsonParser p, DeserializationContext ctxt) {
			var test = p.getText();
			if (StringUtils.isEmpty(test)) {
				return List.of();
			}
			return List.of(test.split(" "));
		}
	}
}
