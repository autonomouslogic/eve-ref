package com.autonomouslogic.everef.esi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.StringUtils;

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

	public static final class ExpiresOnDeserializer extends JsonDeserializer<Instant> {
		@Override
		public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			return LocalDateTime.parse(p.getText()).atZone(ZoneOffset.UTC).toInstant();
		}
	}

	public static final class ScopesDeserializer extends JsonDeserializer<List<String>> {
		@Override
		public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			var test = p.getText();
			if (StringUtils.isEmpty(test)) {
				return List.of();
			}
			return List.of(test.split(" "));
		}
	}
}
