package com.autonomouslogic.everef.cli.basiclogin;

import com.autonomouslogic.everef.esi.EsiAuthHelper;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import javax.inject.Inject;
import javax.inject.Provider;

@Factory
public class BasicLoginFactory {
	@Inject
	protected Provider<EsiAuthHelper> esiAuthHelperProvider;

	@Inject
	protected BasicLoginFactory() {}

	@Bean
	@Replaces
	public EsiAuthHelper esiAuthHelper() {
		return esiAuthHelperProvider.get();
	}
}
