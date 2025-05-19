package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.model.api.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Singleton
@Log4j2
public class ClientErrorHandler implements io.helidon.webserver.http.ErrorHandler<ClientException> {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected ClientErrorHandler() {}

	@Override
	@SneakyThrows
	public void handle(ServerRequest req, ServerResponse res, ClientException e) {
		res.status(Status.BAD_REQUEST_400)
				.send(objectMapper.writeValueAsString(ApiError.builder()
								.message(ExceptionUtils.getMessage(e))
								.build()) + "\n");
	}
}
