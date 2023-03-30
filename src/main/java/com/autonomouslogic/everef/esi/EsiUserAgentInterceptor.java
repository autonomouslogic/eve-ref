package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.UserAgentInterceptor;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

/**
 * Adds a user agent header.
 */
@Singleton
@Log4j2
public class EsiUserAgentInterceptor extends UserAgentInterceptor {
	@Inject
	protected EsiUserAgentInterceptor() {
		super(Configs.ESI_USER_AGENT.getRequired());
	}
}
