package com.autonomouslogic.everef.esi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.scribejava.core.model.OAuth2AccessToken;
import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Maybe;
import javax.inject.Singleton;

@Module
public class MockEsiAuthHelperModule {
	@Provides
	@Singleton
	public EsiAuthHelper esiAuthHelper() {
		var mock = mock(EsiAuthHelper.class);
		when(mock.getTokenForOwnerHash(any())).thenReturn(Maybe.just(new OAuth2AccessToken("oath2-token")));
		return mock;
	}
}
