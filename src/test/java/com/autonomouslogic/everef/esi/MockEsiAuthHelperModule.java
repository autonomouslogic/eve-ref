package com.autonomouslogic.everef.esi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.github.scribejava.core.model.OAuth2AccessToken;
import dagger.Module;
import dagger.Provides;
import java.util.Optional;
import javax.inject.Singleton;

@Module
public class MockEsiAuthHelperModule {
	@Provides
	@Singleton
	public EsiAuthHelper esiAuthHelper() {
		var mock = mock(EsiAuthHelper.class);
		lenient().when(mock.getTokenForOwnerHash(any())).thenReturn(Optional.of(new OAuth2AccessToken("oauth2-token")));
		lenient().when(mock.getTokenStringForOwnerHash(any())).thenReturn("oauth2-token");
		lenient()
				.when(mock.verify(any()))
				.thenReturn(EsiVerifyResponse.builder()
						.characterId(1000000000)
						.characterName("Test Character")
						.build());
		return mock;
	}
}
