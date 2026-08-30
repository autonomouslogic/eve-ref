package com.autonomouslogic.everef.inject;

import static com.autonomouslogic.everef.config.Configs.REF_DATA_BASE_URL;

import com.autonomouslogic.everef.openapi.refdata.api.RefdataApi;
import com.autonomouslogic.everef.openapi.refdata.invoker.ApiClient;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class RefDataApiModule {
	@Provides
	@Singleton
	public RefdataApi refdataApi() {
		var base = REF_DATA_BASE_URL.getRequired();
		var api = new ApiClient();
		api.setScheme(base.getScheme());
		api.setHost(base.getHost());
		api.setPort(base.getPort());
		api.setBasePath(base.getPath());
		// The generated client uses Jackson 2 which doesn't understand tools.jackson.databind.annotation.JsonNaming.
		// Set snake_case globally so model classes using @JsonNaming(SnakeCaseStrategy) are deserialized correctly.
		var mapper = ApiClient.createDefaultObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		api.setObjectMapper(mapper);
		return new RefdataApi(api);
	}
}
