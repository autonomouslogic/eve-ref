package com.autonomouslogic.everef.cli.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
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
public class HealthcheckDecoratorTest {
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
		server.start(TestDataUtil.TEST_PORT);
		for (int i = 0; i < 4; i++) {
			server.enqueue(new MockResponse().setResponseCode(204));
		}

		lenient().doNothing().when(testCommand).run();
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldCallDelegateWhenDisabled() {
		VirtualThreads.onVirtualThread(() -> healthcheckDecorator.decorate(testCommand).run());
		verify(testCommand).run();
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/finish?key=val")
	void shouldCallFinishUrl() {
		VirtualThreads.onVirtualThread(() -> healthcheckDecorator.decorate(testCommand).run());
		verify(testCommand).run();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "POST", "/finish?key=val", null);
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_START_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/start?key=val")
	void shouldCallStartUrl() {
		VirtualThreads.onVirtualThread(() -> healthcheckDecorator.decorate(testCommand).run());
		verify(testCommand).run();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "POST", "/start?key=val", null);
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_FAIL_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/fail?key=val")
	void shouldCallFailUrlOnError() {
		doThrow(new RuntimeException("test error message")).when(testCommand).run();
		var error = assertThrows(
				RuntimeException.class,
				() -> VirtualThreads.onVirtualThread(() -> healthcheckDecorator.decorate(testCommand).run()));
		assertEquals("test error message", error.getMessage());
		verify(testCommand).run();
		var request = server.takeRequest();
		testDataUtil.assertRequest(request, "POST", "/fail?key=val", null);
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_LOG_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/log?key=val")
	void shouldCallLogUrlOnError() {
		doThrow(new RuntimeException("test error message")).when(testCommand).run();
		var error = assertThrows(
				RuntimeException.class,
				() -> VirtualThreads.onVirtualThread(() -> healthcheckDecorator.decorate(testCommand).run()));
		assertEquals("test error message", error.getMessage());
		verify(testCommand).run();
		var request = server.takeRequest();
		testDataUtil.assertRequest(
				request,
				"POST",
				"/log?key=val",
				s -> assertTrue(s.contains("java.lang.RuntimeException: test error message"), s));
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/finish?key=val")
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_START_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/start?key=val")
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_FAIL_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/fail?key=val")
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_LOG_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/log?key=val")
	void shouldCallSequenceOnSuccess() {
		VirtualThreads.onVirtualThread(() -> healthcheckDecorator.decorate(testCommand).run());
		verify(testCommand).run();
		var start = server.takeRequest();
		testDataUtil.assertRequest(start, "POST", "/start?key=val", null);
		var finish = server.takeRequest();
		testDataUtil.assertRequest(finish, "POST", "/finish?key=val", null);
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/finish?key=val")
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_START_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/start?key=val")
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_FAIL_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/fail?key=val")
	@SetEnvironmentVariable(
			key = "HEALTH_CHECK_LOG_URL",
			value = "http://localhost:" + TestDataUtil.TEST_PORT + "/log?key=val")
	void shouldCallSequenceOnError() {
		doThrow(new RuntimeException("test error message")).when(testCommand).run();
		var error = assertThrows(
				RuntimeException.class,
				() -> healthcheckDecorator.decorate(testCommand).run());
		assertEquals("test error message", error.getMessage());
		verify(testCommand).run();
		var start = server.takeRequest();
		testDataUtil.assertRequest(start, "POST", "/start?key=val", null);
		var log = server.takeRequest();
		testDataUtil.assertRequest(
				log, "POST", "/log?key=val", body -> assertTrue(body.contains("RuntimeException: test error message")));
		var fail = server.takeRequest();
		testDataUtil.assertRequest(fail, "POST", "/fail?key=val", null);
		testDataUtil.assertNoMoreRequests(server);
	}
}
