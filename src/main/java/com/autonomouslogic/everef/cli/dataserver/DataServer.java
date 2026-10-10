package com.autonomouslogic.everef.cli.dataserver;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.dataserver.DataProxyHandler;
import com.autonomouslogic.everef.service.HealthcheckService;
import com.autonomouslogic.everef.util.SentryUtil;
import io.helidon.common.concurrency.limits.FixedLimit;
import io.helidon.common.concurrency.limits.LimitException;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Streaming proxy for data.everef.net, backed directly by B2. See data-server-plan.md for the full design.
 */
@Log4j2
public class DataServer implements Command {
	private final int port = Configs.HTTP_PORT.getRequired();

	@Inject
	protected DataProxyHandler dataProxyHandler;

	@Inject
	protected HealthcheckService healthcheckService;

	private WebServer server;
	private volatile boolean stopped = false;

	@Inject
	protected DataServer() {}

	@Override
	@SneakyThrows
	public void run() {
		VirtualThreads.checkIsVirtual();
		startServer();
		startPeriodicHealthcheck();
		while (!stopped) {
			Thread.sleep(100);
		}
	}

	public void startServer() {
		var maxConcurrency = Configs.DATA_SERVER_MAX_CONCURRENCY.getRequired();
		var limit = FixedLimit.create(builder -> builder.permits(maxConcurrency).queueLength(0));
		server = WebServer.builder()
				.port(port)
				.host("0.0.0.0")
				.concurrencyLimit(limit)
				.routing(this::routing)
				.build();
		server.start();
		log.info("Server started");
	}

	private void startPeriodicHealthcheck() {
		healthcheckService.startPeriodicPing();
	}

	public void stop() {
		stopped = true;
		healthcheckService.stop();
		server.stop();
	}

	private HttpRouting.Builder routing(HttpRouting.Builder routing) {
		routing = routing.get("/*", dataProxyHandler);
		routing = routing.head("/*", dataProxyHandler);
		routing = routing.any("/*", dataProxyHandler);
		routing = routing.error(LimitException.class, this::handleLimitExceeded);
		routing = routing.error(Exception.class, this::handleUnexpectedError);
		return routing;
	}

	private void handleLimitExceeded(
			io.helidon.webserver.http.ServerRequest req,
			io.helidon.webserver.http.ServerResponse res,
			LimitException e) {
		res.header("Retry-After", "1")
				.status(io.helidon.http.Status.SERVICE_UNAVAILABLE_503)
				.send("Service unavailable\n".getBytes(StandardCharsets.UTF_8));
	}

	private void handleUnexpectedError(
			io.helidon.webserver.http.ServerRequest req, io.helidon.webserver.http.ServerResponse res, Exception e) {
		log.error("Unexpected error handling data-server request", e);
		Sentry.captureException(e, scope -> {
			SentryUtil.configureScope(scope, req, res);
			scope.setLevel(SentryLevel.ERROR);
		});
		res.status(io.helidon.http.Status.INTERNAL_SERVER_ERROR_500)
				.send("Internal server error\n".getBytes(StandardCharsets.UTF_8));
	}
}
