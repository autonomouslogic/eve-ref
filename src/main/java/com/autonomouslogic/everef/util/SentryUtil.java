package com.autonomouslogic.everef.util;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.sentry.IScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SentryUtil {

	public static void configureScope(IScope scope, ServerRequest req, ServerResponse res) {
		scope.setExtra("http.method", req.prologue().method().toString());
		scope.setExtra("http.uri", req.requestedUri().toUri().toString());
	}
}
