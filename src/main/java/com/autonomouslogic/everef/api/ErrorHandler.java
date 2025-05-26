package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.util.SentryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class ErrorHandler implements io.helidon.webserver.http.ErrorHandler<Exception> {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected ErrorHandler() {}

	@Override
	@SneakyThrows
	public void handle(ServerRequest req, ServerResponse res, Exception e) {
		log.error(
				"Error processing request: {} {}",
				req.prologue().method(),
				URLEncoder.encode(req.requestedUri().toUri().toString(), StandardCharsets.UTF_8),
				e);
		Sentry.captureException(e, scope -> {
			SentryUtil.configureScope(scope, req, res);
			scope.setLevel(SentryLevel.ERROR);
		});
		res.status(Status.INTERNAL_SERVER_ERROR_500)
				.send(objectMapper.writeValueAsString(ApiError.builder()
								.message("An internal error occurred")
								.build()) + "\n");
	}
}
