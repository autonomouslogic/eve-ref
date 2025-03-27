package com.autonomouslogic.everef.cli.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log4j2
@Timeout(5)
@SetEnvironmentVariable(key = "SLACK_WEBHOOK_CHANNEL", value = "#channel-name")
@SetEnvironmentVariable(key = "SLACK_WEBHOOK_USERNAME", value = "Slack User")
public class SlackDecoratorTest {
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
		server.start(TestDataUtil.TEST_PORT);
		for (int i = 0; i < 2; i++) {
			server.enqueue(new MockResponse().setResponseCode(204));
		}

		lenient()
				.when(testCommand.runAsync())
				.thenReturn(Completable.timer(1, TimeUnit.SECONDS).observeOn(VirtualThreads.SCHEDULER));
		lenient().when(testCommand.getName()).thenReturn("command-name");
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldCallDelegateWhenDisabled() {
		slackDecorator.decorate(testCommand).run();
		verify(testCommand).run();
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "SLACK_WEBHOOK_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/webhook?key=val")
	void shouldReportSuccess() {
		slackDecorator.decorate(testCommand).run();
		verify(testCommand).runAsync();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "POST", "/webhook?key=val", body -> {
			var payload = decodePayload(body);
			log.info("Payload: " + payload.toPrettyString());
			assertEquals("#channel-name", payload.get("channel").asText());
			assertEquals("Slack User", payload.get("username").asText());
			var message = payload.get("text").asText();
			assertTrue(message.startsWith(":large_green_circle: command-name completed in PT"));
			assertNull(payload.get("attachments"));
			assertDuration(message);
		});
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "SLACK_WEBHOOK_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/webhook?key=val")
	void shouldReportFailure() {
		when(testCommand.runAsync())
				.thenReturn(Completable.timer(1, TimeUnit.SECONDS)
						.observeOn(VirtualThreads.SCHEDULER)
						.andThen(Completable.error(new RuntimeException("test error message"))));
		var error = assertThrows(
				RuntimeException.class,
				() -> slackDecorator.decorate(testCommand).run());
		error.printStackTrace();
		assertEquals("test error message", error.getMessage());
		verify(testCommand).runAsync();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "POST", "/webhook?key=val", body -> {
			var payload = decodePayload(body);
			log.info("Payload: " + payload.toPrettyString());
			assertEquals("#channel-name", payload.get("channel").asText());
			assertEquals("Slack User", payload.get("username").asText());
			var message = payload.get("text").asText();
			assertTrue(message.startsWith(":large_red_square: command-name failed after PT"));
			var attachments = payload.get("attachments");
			assertEquals(1, attachments.size());
			assertEquals(
					"RuntimeException: test error message",
					attachments.get(0).get("text").asText());
			assertEquals("", attachments.get(0).get("fallback").asText());
			assertDuration(message);
		});
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "SLACK_WEBHOOK_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/webhook?key=val")
	@SetEnvironmentVariable(key = "SLACK_REPORT_SUCCESS", value = "false")
	void shouldNotReportSuccessWhenConfiguredNotTo() {
		slackDecorator.decorate(testCommand).run();
		verify(testCommand).runAsync();
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "SLACK_WEBHOOK_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/webhook?key=val")
	@SetEnvironmentVariable(key = "SLACK_REPORT_FAILURE", value = "false")
	void shouldNotReportFailureWhenConfiguredNotTo() {
		when(testCommand.runAsync()).thenReturn(Completable.error(new RuntimeException("test error message")));
		var error = assertThrows(
				RuntimeException.class,
				() -> slackDecorator.decorate(testCommand).run());
		assertEquals("test error message", error.getMessage());
		verify(testCommand).runAsync();
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "SLACK_WEBHOOK_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/webhook?key=val")
	@SetEnvironmentVariable(key = "SLACK_REPORT_FULL_STACKTRACE", value = "true")
	void shouldReportFullStacktracesWhenConfigured() {
		when(testCommand.runAsync()).thenReturn(Completable.error(new RuntimeException("test error message")));
		var error = assertThrows(
				RuntimeException.class,
				() -> slackDecorator.decorate(testCommand).run());
		assertEquals("test error message", error.getMessage());
		verify(testCommand).runAsync();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "POST", "/webhook?key=val", body -> {
			var payload = decodePayload(body);
			log.info("Payload: " + payload.toPrettyString());
			assertEquals("#channel-name", payload.get("channel").asText());
			assertEquals("Slack User", payload.get("username").asText());
			assertTrue(payload.get("text").asText().startsWith(":large_red_square: command-name failed after PT"));
			var attachments = payload.get("attachments");
			assertEquals(1, attachments.size());
			assertTrue(
					attachments
							.get(0)
							.get("text")
							.asText()
							.startsWith(
									"java.lang.RuntimeException: test error message\n\tat com.autonomouslogic.everef.cli.decorator.SlackDecoratorTest"));
			assertEquals("", attachments.get(0).get("fallback").asText());
		});
		testDataUtil.assertNoMoreRequests(server);
	}

	@SneakyThrows
	private ObjectNode decodePayload(String payload) {
		if (payload.startsWith("payload=")) {
			payload = payload.substring(8);
		}
		payload = URLDecoder.decode(payload, StandardCharsets.UTF_8);
		return (ObjectNode) objectMapper.readTree(payload);
	}

	private static void assertDuration(String message) {
		var matcher = Pattern.compile(".*(PT[0-9A-Z\\.]+).*").matcher(message);
		assertTrue(matcher.matches());
		var match = matcher.group(1);
		var duration = Duration.parse(match);
		assertTrue(duration.toMillis() >= 1000, duration.toString());
	}
}
