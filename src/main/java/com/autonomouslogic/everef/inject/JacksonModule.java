package com.autonomouslogic.everef.inject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import org.snakeyaml.engine.v2.api.LoadSettings;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Module
public class JacksonModule {
	@Provides
	@Singleton
	public JsonMapper jsonMapper() {
		return JsonMapper.builder()
				.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
				.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
				.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
				.enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
				.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
				.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
				.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
				.changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE))
				.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
				.enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
				.build();
	}

	@Provides
	@Singleton
	public CsvMapper csvMapper() {
		return CsvMapper.builder()
				.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
				.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
				.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
				.changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE))
				.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
				.enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
				.build();
	}

	@Provides
	@Singleton
	@Named("yaml")
	public ObjectMapper yamlMapper() {
		var loadSettings = LoadSettings.builder()
				.setCodePointLimit(1024 * 1024 * 1024) // 1 GiB
				.build();
		var factory = YAMLFactory.builder().loadSettings(loadSettings).build();
		return YAMLMapper.builder(factory)
				.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
				.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
				.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
				.changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE))
				.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
				.enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
				.build();
	}
}
