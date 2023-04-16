package com.autonomouslogic.everef.inject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import org.yaml.snakeyaml.LoaderOptions;

/**
 *
 */
@Module
public class JacksonModule {
	@Provides
	@Singleton
	public ObjectMapper objectMapper() {
		return configure(new ObjectMapper());
	}

	@Provides
	@Singleton
	@Named("yaml")
	public ObjectMapper yamlMapper() {
		var loaderOptions = new LoaderOptions();
		loaderOptions.setCodePointLimit(1024 * 1024 * 1024); // 1 GiB.
		var factory = YAMLFactory.builder().loaderOptions(loaderOptions).build();
		return configure(new ObjectMapper(factory));
	}

	private ObjectMapper configure(ObjectMapper objectMapper) {
		objectMapper.disable(MapperFeature.AUTO_DETECT_CREATORS);
		objectMapper.disable(MapperFeature.AUTO_DETECT_FIELDS);
		objectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS);
		objectMapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS);
		objectMapper.disable(MapperFeature.AUTO_DETECT_SETTERS);
		objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.enable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);
		objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
		objectMapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
		objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
		objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
		objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		objectMapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
		objectMapper.setVisibility(objectMapper.getVisibilityChecker().with(JsonAutoDetect.Visibility.NONE));
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
		objectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
		objectMapper.enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
		return objectMapper;
	}
}
