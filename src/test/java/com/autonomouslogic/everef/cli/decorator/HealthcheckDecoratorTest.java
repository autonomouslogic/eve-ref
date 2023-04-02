package com.autonomouslogic.everef.cli.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import io.reactivex.rxjava3.core.Completable;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log4j2
@Timeout(5)
public class HealthcheckDecoratorTest {
	private static final int PORT = 20730;

	@Mock
	Command testCommand;

	@Inject
	HealthcheckDecorator healthcheckDecorator;

	@Inject
	TestDataUtil testDataUtil;

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
		healthcheckDecorator.decorate(testCommand).run().blockingAwait();
		verify(testCommand).run();
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HEALTH_CHECK_URL", value = "http://localhost:" + PORT + "/finish?key=val")
	void shouldCallFinishUrl() {
		healthcheckDecorator.decorate(testCommand).run().blockingAwait();
		verify(testCommand).run();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "/finish?key=val");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HEALTH_CHECK_START_URL", value = "http://localhost:" + PORT + "/start?key=val")
	void shouldCallStartUrl() {
		healthcheckDecorator.decorate(testCommand).run().blockingAwait();
		verify(testCommand).run();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "/start?key=val");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HEALTH_CHECK_FAIL_URL", value = "http://localhost:" + PORT + "/fail?key=val")
	void shouldCallFailUrlOnError() {
		when(testCommand.run()).thenReturn(Completable.error(new RuntimeException("test error message")));
		var error = assertThrows(
				RuntimeException.class,
				() -> healthcheckDecorator.decorate(testCommand).run().blockingAwait());
		assertEquals("test error message", error.getMessage());
		verify(testCommand).run();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "/fail?key=val");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HEALTH_CHECK_LOG_URL", value = "http://localhost:" + PORT + "/log?key=val")
	void shouldCallLogUrlOnError() {
		when(testCommand.run()).thenReturn(Completable.error(new RuntimeException("test error message")));
		var error = assertThrows(
				RuntimeException.class,
				() -> healthcheckDecorator.decorate(testCommand).run().blockingAwait());
		assertEquals("test error message", error.getMessage());
		verify(testCommand).run();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "/log?key=val", "RuntimeException: test error message");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HEALTH_CHECK_URL", value = "http://localhost:" + PORT + "/finish?key=val")
	@SetEnvironmentVariable(key = "HEALTH_CHECK_START_URL", value = "http://localhost:" + PORT + "/start?key=val")
	@SetEnvironmentVariable(key = "HEALTH_CHECK_FAIL_URL", value = "http://localhost:" + PORT + "/fail?key=val")
	@SetEnvironmentVariable(key = "HEALTH_CHECK_LOG_URL", value = "http://localhost:" + PORT + "/log?key=val")
	void shouldCallSequenceOnSuccess() {
		healthcheckDecorator.decorate(testCommand).run().blockingAwait();
		verify(testCommand).run();
		var start = server.takeRequest();
		testDataUtil.assertRequest(start, "/start?key=val");
		var finish = server.takeRequest();
		testDataUtil.assertRequest(finish, "/finish?key=val");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HEALTH_CHECK_URL", value = "http://localhost:" + PORT + "/finish?key=val")
	@SetEnvironmentVariable(key = "HEALTH_CHECK_START_URL", value = "http://localhost:" + PORT + "/start?key=val")
	@SetEnvironmentVariable(key = "HEALTH_CHECK_FAIL_URL", value = "http://localhost:" + PORT + "/fail?key=val")
	@SetEnvironmentVariable(key = "HEALTH_CHECK_LOG_URL", value = "http://localhost:" + PORT + "/log?key=val")
	void shouldCallSequenceOnError() {
		when(testCommand.run()).thenReturn(Completable.error(new RuntimeException("test error message")));
		var error = assertThrows(
				RuntimeException.class,
				() -> healthcheckDecorator.decorate(testCommand).run().blockingAwait());
		assertEquals("test error message", error.getMessage());
		verify(testCommand).run();
		var start = server.takeRequest();
		testDataUtil.assertRequest(start, "/start?key=val");
		var log = server.takeRequest();
		testDataUtil.assertRequest(log, "/log?key=val", "RuntimeException: test error message");
		var fail = server.takeRequest();
		testDataUtil.assertRequest(fail, "/fail?key=val");
		testDataUtil.assertNoMoreRequests(server);
	}
}
