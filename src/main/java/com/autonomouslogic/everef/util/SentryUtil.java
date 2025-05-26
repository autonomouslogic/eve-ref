package com.autonomouslogic.everef.util;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.sentry.IScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SentryUtil {

	public static void configureScope(IScope scope, ServerRequest req, ServerResponse res) {
		scope.setContexts("http.path", req.path());
		scope.setContexts("http.responseCode", res.status().code());
	}
}
