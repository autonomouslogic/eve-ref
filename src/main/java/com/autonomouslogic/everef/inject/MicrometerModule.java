package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
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
	private static final int PORT = 9090;

	@Provides
	@Singleton
	public MeterRegistry meterRegistry() {
		var registry = new CompositeMeterRegistry();
		createPrometheusRegistry().ifPresent(registry::add);
		return registry;
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
		log.info("Starting Prometheus server in port {}", PORT);
		try {
			new HTTPServer(new InetSocketAddress(PORT), prometheusMeterRegistry.getPrometheusRegistry(), true);
		} catch (IOException e) {
			throw new IOException("Failed to start Prometheus server", e);
		}
	}
}
