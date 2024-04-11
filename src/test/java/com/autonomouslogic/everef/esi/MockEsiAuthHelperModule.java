package com.autonomouslogic.everef.esi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.github.scribejava.core.model.OAuth2AccessToken;
import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

import javax.inject.Singleton;

@Module
public class MockEsiAuthHelperModule {
	@Provides
	@Singleton
	public EsiAuthHelper esiAuthHelper() {
		var mock = mock(EsiAuthHelper.class);
		lenient().when(mock.getTokenForOwnerHash(any())).thenReturn(Maybe.just(new OAuth2AccessToken("oauth2-token")));
		lenient().when(mock.getTokenStringForOwnerHash(any())).thenReturn(Single.just("oauth2-token"));
		return mock;
	}
}
