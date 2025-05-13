package com.autonomouslogic.everef.api;

import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class StandardHandlers {
	public static final Handler HTTP_METHOD_NOT_ALLOWED = new Handler() {
		@Override
		public void handle(ServerRequest serverRequest, ServerResponse serverResponse) throws Exception {
			serverResponse.status(405).send();
		}
	};
}
