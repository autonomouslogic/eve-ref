package com.autonomouslogic.everef.cli.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log4j2
@Timeout(5)
@SetEnvironmentVariable(key = "SLACK_WEBHOOK_CHANNEL", value = "#channel-name")
@SetEnvironmentVariable(key = "SLACK_WEBHOOK_USERNAME", value = "Slack User")
public class SlackDecoratorTest {
	private static final int PORT = 30150;

	@Mock
	Command testCommand;

	@Inject
	SlackDecorator slackDecorator;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	TestDataUtil testDataUtil;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.start(PORT);
		for (int i = 0; i < 2; i++) {
			server.enqueue(new MockResponse().setResponseCode(204));
		}

		when(testCommand.run()).thenReturn(Completable.complete());
		Mockito.lenient().when(testCommand.getName()).thenReturn("command-name");
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldCallDelegateWhenDisabled() {
		slackDecorator.decorate(testCommand).run().blockingAwait();
		verify(testCommand).run();
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "SLACK_WEBHOOK_URL", value = "http://localhost:" + PORT + "/webhook?key=val")
	void shouldReportSuccess() {
		slackDecorator.decorate(testCommand).run().blockingAwait();
		verify(testCommand).run();
		var request = server.takeRequest();
		var body = testDataUtil.assertRequest(request, "/webhook?key=val");
		var payload = decodePayload(body);
		log.info("Payload: " + payload);
		assertEquals("#channel-name", payload.get("channel").asText());
		assertEquals("Slack User", payload.get("username").asText());
		assertEquals("command-name completed", payload.get("text").asText());
		var attachments = payload.get("attachments");
		assertEquals(1, attachments.size());
		assertTrue(attachments.get(0).get("fallback").asText().startsWith("command-name completed in PT"));
		assertTrue(attachments.get(0).get("text").asText().startsWith("command-name completed in PT"));
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "SLACK_WEBHOOK_URL", value = "http://localhost:" + PORT + "/webhook?key=val")
	void shouldReportFailure() {
		when(testCommand.run()).thenReturn(Completable.error(new RuntimeException("test error message")));
		var error = assertThrows(
				RuntimeException.class,
				() -> slackDecorator.decorate(testCommand).run().blockingAwait());
		assertEquals("test error message", error.getMessage());
		verify(testCommand).run();
		var request = server.takeRequest();
		var body = testDataUtil.assertRequest(request, "/webhook?key=val");
		var payload = decodePayload(body);
		log.info("Payload: " + payload);
		assertEquals("#channel-name", payload.get("channel").asText());
		assertEquals("Slack User", payload.get("username").asText());
		assertEquals("command-name failed", payload.get("text").asText());
		var attachments = payload.get("attachments");
		assertEquals(1, attachments.size());
		assertTrue(attachments.get(0).get("fallback").asText().startsWith("command-name failed after PT"));
		assertTrue(attachments.get(0).get("text").asText().startsWith("command-name failed after PT"));
		testDataUtil.assertNoMoreRequests(server);
	}

	@SneakyThrows
	private ObjectNode decodePayload(byte[] payload) {
		var str = new String(payload);
		if (str.startsWith("payload=")) {
			str = str.substring(8);
		}
		str = URLDecoder.decode(str, StandardCharsets.UTF_8);
		return (ObjectNode) objectMapper.readTree(str);
	}
}
