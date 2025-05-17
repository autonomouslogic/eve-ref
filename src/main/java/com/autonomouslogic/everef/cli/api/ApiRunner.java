package com.autonomouslogic.everef.cli.api;

import com.autonomouslogic.everef.api.ClientErrorHandler;
import com.autonomouslogic.everef.api.ClientException;
import com.autonomouslogic.everef.api.ErrorHandler;
import com.autonomouslogic.everef.api.IndustryCostHandler;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.service.RefDataService;
import io.helidon.common.concurrency.limits.AimdLimit;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ApiRunner implements Command {
	private final int port = Configs.HTTP_PORT.getRequired();

	@Inject
	protected IndustryCostHandler industryCostHandler;

	@Inject
	protected ErrorHandler errorHandler;

	@Inject
	protected ClientErrorHandler clientErrorHandler;

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected MarketPriceService marketPriceService;

	private WebServer server;
	private volatile boolean stopped = false;

	@Inject
	protected ApiRunner() {}

	@Override
	@SneakyThrows
	public void run() {
		startServices();
		startServer();
		while (!stopped) {
			Thread.sleep(100);
		}
	}

	@SneakyThrows
	public void startServices() {
		refDataService.init();
		marketPriceService.init();
		while (!refDataService.isReady() || !marketPriceService.isReady()) {
			Thread.sleep(10);
		}
	}

	public void startServer() {
		// It's possible for Helidon MP to do all of this via auto-discovery, but I wasn't able to immediately figure
		// out how to do it. I also want to maintain Dagger for injections and whatnot.
		// At the time of writing, there's on endpoint. This is good enough.
		server = WebServer.builder()
				.port(port)
				.host("0.0.0.0")
				.routing(this::routing)
				.concurrencyLimit(AimdLimit.create())
				.build();
		server.start();
	}

	public void stop() {
		stopped = true;
		server.stop();
		refDataService.stop();
		marketPriceService.stop();
	}

	private HttpRouting.Builder routing(HttpRouting.Builder routing) {
		return routing.register(industryCostHandler)
				.error(ClientException.class, clientErrorHandler)
				.error(Exception.class, errorHandler);
	}
}
