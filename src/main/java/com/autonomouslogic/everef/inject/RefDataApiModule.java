package com.autonomouslogic.everef.inject;

import static com.autonomouslogic.everef.config.Configs.REF_DATA_BASE_URL;

import com.autonomouslogic.everef.openapi.refdata.apis.RefdataApi;
import com.autonomouslogic.everef.openapi.refdata.invoker.ApiClient;
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
		return new RefdataApi(api);
	}
}
