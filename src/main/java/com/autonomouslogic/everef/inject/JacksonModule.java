package com.autonomouslogic.everef.inject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 *
 */
@Module
@Singleton
public class JacksonModule {
	@Provides
	@Singleton
	public ObjectMapper objectMapper() {
		return configure(new ObjectMapper());
	}

	private ObjectMapper configure(ObjectMapper objectMapper) {
		objectMapper.configure(MapperFeature.AUTO_DETECT_CREATORS, false);
		objectMapper.configure(MapperFeature.AUTO_DETECT_FIELDS, false);
		objectMapper.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
		objectMapper.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
		objectMapper.configure(MapperFeature.AUTO_DETECT_SETTERS, false);
		objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
		objectMapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true);
		objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
		objectMapper.setVisibility(objectMapper.getVisibilityChecker().with(JsonAutoDetect.Visibility.NONE));
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper;
	}
}
