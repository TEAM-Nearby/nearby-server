// 예상하지 못한 500 오류만 Sentry 이벤트로 수집되는지 검증하는 통합 테스트
package com.sopt.nearby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.user.exception.PhoneVerificationSendLimitExceededException;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

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
	@WithMockUser
	void capturesDirectServerErrorResponseOnce() throws Exception {
		mockMvc.perform(get("/test/sentry/direct-server-error")
				.header("X-Request-Id", REQUEST_ID))
				.andExpect(status().isInternalServerError());

		assertThat(CAPTURED_EVENTS).hasSize(1);
		assertThat(CAPTURED_EVENTS.getFirst().getMessage().getFormatted())
				.contains("HTTP server error")
				.contains("status=500");
	}

	@Test
	@WithMockUser
	void capturesGooglePlaceApiExceptionOnce() throws Exception {
		mockMvc.perform(get("/test/sentry/google-place-error"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("GOOGLE_PLACE_API_ERROR"));

		assertCapturedException(GooglePlaceApiException.class);
	}

	@Test
	@WithMockUser
	void capturesPhoneVerificationSendLimitExceededExceptionOnce() throws Exception {
		mockMvc.perform(get("/test/sentry/phone-verification-send-limit-error"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED"));

		assertCapturedException(PhoneVerificationSendLimitExceededException.class);
	}

	@Test
	void capturesExceptionThrownBeforeSpringMvcOnce() {
		assertThatThrownBy(() -> mockMvc.perform(get("/test/sentry/filter-error")
				.header("X-Request-Id", REQUEST_ID)))
				.hasRootCauseInstanceOf(IllegalStateException.class);

		assertCapturedException(IllegalStateException.class);
		assertThat(CAPTURED_EVENTS.getFirst().getMessage().getFormatted()).contains("status=500");
	}

	@Test
	void doesNotCaptureUnauthorizedResponse() throws Exception {
		mockMvc.perform(get("/test/sentry/business-error"))
				.andExpect(status().isUnauthorized());

		assertThat(CAPTURED_EVENTS).isEmpty();
	}

	@Test
	void doesNotCaptureReturnedHealthServerError() throws Exception {
		mockMvc.perform(get("/actuator/health/test-status"))
				.andExpect(status().isServiceUnavailable());

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

	private static void assertCapturedException(final Class<? extends Throwable> exceptionType) {
		assertThat(CAPTURED_EVENTS).hasSize(1);
		assertThat(rootCause(CAPTURED_EVENTS.getFirst().getThrowable())).isInstanceOf(exceptionType);
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

		@Bean
		FilterRegistrationBean<OncePerRequestFilter> failingFilter() {
			OncePerRequestFilter filter = new OncePerRequestFilter() {
				@Override
				protected void doFilterInternal(
						final HttpServletRequest request,
						final HttpServletResponse response,
						final jakarta.servlet.FilterChain filterChain
				) throws ServletException, IOException {
					if (request.getRequestURI().equals("/test/sentry/filter-error")) {
						throw new ServletException(new IllegalStateException("filter unexpected"));
					}
					if (request.getRequestURI().equals("/actuator/health/test-status")) {
						response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
						return;
					}
					filterChain.doFilter(request, response);
				}
			};
			FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
			return registration;
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

		@GetMapping("/test/sentry/direct-server-error")
		ResponseEntity<Void> directServerError() {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		@GetMapping("/test/sentry/google-place-error")
		void googlePlaceError() {
			throw new GooglePlaceApiException();
		}

		@GetMapping("/test/sentry/phone-verification-send-limit-error")
		void phoneVerificationSendLimitError() {
			throw new PhoneVerificationSendLimitExceededException();
		}
	}
}
