package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DiscordNotifier {
	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected ObjectMapper objectMapper;

	private final Optional<URI> discordUrl = Configs.DISCORD_WEBHOOK_URL.get();

	@Inject
	protected DiscordNotifier() {}

	@SneakyThrows
	public void notifyDiscord(String message) {
		if (discordUrl.isEmpty()) {
			log.debug("No Discord webhook URL configured");
			return;
		}
		log.debug("Notifying Discord");
		var body = objectMapper.createObjectNode();
		body.put("content", message);
		log.trace("Discord notification: {}", body);
		try (var response = okHttpWrapper.post(
				discordUrl.get().toString(),
				objectMapper.writeValueAsBytes(body),
				r -> r.header("Content-Type", "application/json"))) {
			if (response.code() < 200 || response.code() >= 300) {
				log.warn("Error notifying Discord: {}", response);
			} else {
				log.debug("Discord notified: {}", response);
			}
		}
	}
}
