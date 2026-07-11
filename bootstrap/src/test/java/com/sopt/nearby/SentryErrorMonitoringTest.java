// 예상하지 못한 500 오류만 Sentry 이벤트로 수집되는지 검증하는 통합 테스트
package com.sopt.nearby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = {
		"sentry.dsn=http://public@localhost/1",
		"sentry.environment=test",
		"management.health.redis.enabled=false"
})
@AutoConfigureMockMvc
@Import({
		SentryErrorMonitoringTest.SentryCaptureConfiguration.class,
		SentryErrorMonitoringTest.TestEndpoint.class
})
class SentryErrorMonitoringTest {

	private static final String REQUEST_ID = "sentry-test-request-id";
	private static final List<SentryEvent> CAPTURED_EVENTS = new CopyOnWriteArrayList<>();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SentryOptions sentryOptions;

	@BeforeEach
	void clearCapturedEvents() {
		CAPTURED_EVENTS.clear();
	}

	@AfterAll
	static void closeSentry() {
		Sentry.close();
	}

	@Test
	@WithMockUser
	void capturesUnhandledServerErrorOnce() throws Exception {
		mockMvc.perform(get("/test/sentry/unhandled")
				.header("X-Request-Id", REQUEST_ID))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));

		assertThat(CAPTURED_EVENTS).hasSize(1);
		SentryEvent event = CAPTURED_EVENTS.getFirst();
		assertThat(event.getThrowable()).isNotNull();
		assertThat(rootCause(event.getThrowable()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("unexpected");
		assertThat(event.getRequest()).isNotNull();
		assertThat(event.getRequest().getMethod()).isEqualTo("GET");
		assertThat(event.getRequest().getUrl()).endsWith("/test/sentry/unhandled");
		assertThat(event.getBreadcrumbs()).anySatisfy(breadcrumb ->
				assertThat(breadcrumb.getMessage()).contains("HTTP request started"));
		Object mdcContext = event.getContexts().get("MDC");
		assertThat(mdcContext).isInstanceOf(Map.class);
		assertThat(((Map<?, ?>) mdcContext).get("requestId")).isEqualTo(REQUEST_ID);
	}

	@Test
	@WithMockUser
	void doesNotCaptureBusinessException() throws Exception {
		mockMvc.perform(get("/test/sentry/business-error"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_MEETING_ID"));

		assertThat(CAPTURED_EVENTS).isEmpty();
	}

	@Test
	void disablesSensitiveDataAndPerformanceTracing() {
		assertThat(sentryOptions.isSendDefaultPii()).isFalse();
		assertThat(sentryOptions.getMaxRequestBodySize()).isEqualTo(SentryOptions.RequestSize.NONE);
		assertThat(sentryOptions.getTracesSampleRate()).isNull();
		assertThat(sentryOptions.getEnvironment()).isEqualTo("test");
	}

	private static Throwable rootCause(final Throwable throwable) {
		Throwable rootCause = throwable;
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		return rootCause;
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class SentryCaptureConfiguration {

		@Bean
		SentryOptions.BeforeSendCallback captureSentryEvents() {
			return (event, hint) -> {
				CAPTURED_EVENTS.add(event);
				return null;
			};
		}
	}

	@RestController
	static class TestEndpoint {

		@GetMapping("/test/sentry/unhandled")
		void unhandledError() {
			throw new IllegalStateException("unexpected");
		}

		@GetMapping("/test/sentry/business-error")
		void businessError() {
			throw new InvalidCompanionMeetingIdException();
		}
	}
}
