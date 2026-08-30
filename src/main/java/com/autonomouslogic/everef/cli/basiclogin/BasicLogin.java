package com.autonomouslogic.everef.cli.basiclogin;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import io.helidon.webserver.WebServer;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BasicLogin implements Command {
	private final int port = Configs.HTTP_PORT.getRequired();

	@Inject
	protected BasicLoginController basicLoginController;

	@Inject
	protected BasicLogin() {}

	@Override
	@SneakyThrows
	public void run() {
		VirtualThreads.checkThread();
		Configs.EVE_OAUTH_CLIENT_ID.getRequired();
		Configs.EVE_OAUTH_SECRET_KEY.getRequired();

		var server = WebServer.builder()
				.port(port)
				.host("0.0.0.0")
				.routing(rules -> rules.register(basicLoginController))
				.build();
		server.start();
		log.info("Server started on port {}", port);

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		server.stop();
	}
}
