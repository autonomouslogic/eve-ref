package com.autonomouslogic.everef.model;

import com.autonomouslogic.dynamomapper.annotations.DynamoPrimaryKey;
import com.autonomouslogic.dynamomapper.annotations.DynamoTableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A character logged in with the EVE SSO.
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@DynamoTableName("everef-logins")
public class CharacterLogin {
	@DynamoPrimaryKey
	@JsonProperty
	String characterOwnerHash;

	@JsonProperty
	long characterId;

	@JsonProperty
	String characterName;

	@JsonProperty
	String refreshToken;

	@JsonProperty
	List<String> scopes;
}
