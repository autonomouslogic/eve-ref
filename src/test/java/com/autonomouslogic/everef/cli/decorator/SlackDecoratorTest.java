package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import io.reactivex.rxjava3.core.Completable;
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
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.start(PORT);
		for (int i = 0; i < 4; i++) {
			server.enqueue(new MockResponse().setResponseCode(204));
		}

		when(testCommand.run()).thenReturn(Completable.complete());
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
		noMoreRequests();
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "SLACK_WEBHOOK_URL", value = "http://localhost:" + PORT + "/webhook?key=val")
	void shouldReportSuccess() {
		slackDecorator.decorate(testCommand).run().blockingAwait();
		verify(testCommand).run();
		var request = server.takeRequest();
		var body = assertRequest(request, "/finish?key=val");
		assertEquals("TODO", body);
		noMoreRequests();
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
		var body = assertRequest(request, "/webhook?key=val");
		assertEquals("TODO", body);
		noMoreRequests();
	}

	private static String assertRequest(RecordedRequest request, String path) {
		assertEquals(path, request.getPath());
		assertEquals("POST", request.getMethod());
		assertEquals("everef.net", request.getHeader("user-agent"));
		return request.getBody().readUtf8();
	}

	@SneakyThrows
	private void noMoreRequests() {
		assertNull(server.takeRequest(1, TimeUnit.MILLISECONDS));
	}
}
