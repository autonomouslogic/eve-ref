package com.autonomouslogic.everef.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import io.swagger.v3.oas.integration.api.ObjectMapperProcessor;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Bridges Jackson 3's @JsonNaming annotation to swagger-core's Jackson 2 ObjectMapper.
 * swagger-core uses Jackson 2 for spec generation and reads com.fasterxml.jackson.databind.annotation.JsonNaming,
 * but model classes use tools.jackson.databind.annotation.JsonNaming (Jackson 3).
 * This processor installs a custom AnnotationIntrospector that recognises both.
 */
public class SnakeCaseObjectMapperProcessor implements ObjectMapperProcessor {
	@Override
	public void processJsonObjectMapper(ObjectMapper mapper) {
		mapper.setAnnotationIntrospector(new JsonNamingAnnotationIntrospector());
	}

	private static class JsonNamingAnnotationIntrospector extends JacksonAnnotationIntrospector {
		@Override
		public Object findNamingStrategy(AnnotatedClass ac) {
			var base = super.findNamingStrategy(ac);
			if (base != null) return base;
			var j3Naming = ac.getRawType().getAnnotation(JsonNaming.class);
			if (j3Naming != null && j3Naming.value() == SnakeCaseStrategy.class) {
				return PropertyNamingStrategies.SNAKE_CASE;
			}
			return null;
		}
	}
}
