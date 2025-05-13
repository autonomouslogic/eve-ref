package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
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
		log.warn("Error processing request: {}", req.requestedUri().toString(), e);
		res.status(500)
				.send(objectMapper.writeValueAsBytes(
						ApiError.builder().message("An internal error occurred").build()));
	}
}
