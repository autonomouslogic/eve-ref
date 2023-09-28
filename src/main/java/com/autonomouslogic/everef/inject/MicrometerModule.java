package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Module
@Log4j2
public class MicrometerModule {
	@Provides
	@Singleton
	public MeterRegistry meterRegistry() {
		var registry = new CompositeMeterRegistry();
		createGraphiteRegistry().ifPresent(registry::add);
		createPrometheusRegistry().ifPresent(registry::add);
		return registry;
	}

	private Optional<GraphiteMeterRegistry> createGraphiteRegistry() {
		if (!Configs.GRAPHITE_ENABLED.getRequired()) {
			return Optional.empty();
		}
		var host = Configs.GRAPHITE_HOST.getRequired();
		var port = Configs.GRAPHITE_PORT.getRequired();
		var registry = new GraphiteMeterRegistry(
				new GraphiteConfig() {
					@Override
					public String host() {
						return host;
					}

					@Override
					public int port() {
						return port;
					}

					@Override
					public String get(String key) {
						return null;
					}
				},
				Clock.SYSTEM);
		return Optional.of(registry);
	}

	private Optional<PrometheusMeterRegistry> createPrometheusRegistry() {
		if (!Configs.PROMETHEUS_ENABLED.getRequired()) {
			return Optional.empty();
		}
		var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		startPrometheusServer(registry);
		return Optional.of(registry);
	}

	@SneakyThrows
	private void startPrometheusServer(PrometheusMeterRegistry prometheusMeterRegistry) {
		var port = Configs.PROMETHEUS_PORT.getRequired();
		log.debug("Starting Prometheus server in port {}", port);
		try {
			var server =
					new HTTPServer(new InetSocketAddress(port), prometheusMeterRegistry.getPrometheusRegistry(), true);
			log.info("Prometheus server started on port {}", server.getPort());
		} catch (IOException e) {
			throw new IOException("Failed to start Prometheus server", e);
		}
	}
}
