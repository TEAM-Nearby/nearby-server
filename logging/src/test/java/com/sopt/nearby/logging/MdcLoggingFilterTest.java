// 요청 식별자를 MDC와 응답 헤더에 전파하는 필터를 검증하는 테스트
package com.sopt.nearby.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class MdcLoggingFilterTest {

	@Test
	void putsExistingRequestIdHeaderIntoMdcAndResponseHeader() throws ServletException, IOException {
		MdcLoggingFilter filter = new MdcLoggingFilter(new FixedRequestIdGenerator("generated-request-id"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> requestIdInsideChain = new AtomicReference<>();
		request.addHeader(MdcLoggingFilter.REQUEST_ID_HEADER, "client-request-id");

		filter.doFilter(request, response, (servletRequest, servletResponse) ->
				requestIdInsideChain.set(MDC.get(MdcLoggingFilter.MDC_KEY)));

		assertThat(requestIdInsideChain).hasValue("client-request-id");
		assertThat(response.getHeader(MdcLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("client-request-id");
		assertThat(MDC.get(MdcLoggingFilter.MDC_KEY)).isNull();
	}

	@Test
	void generatesRequestIdWhenHeaderIsMissing() throws ServletException, IOException {
		MdcLoggingFilter filter = new MdcLoggingFilter(new FixedRequestIdGenerator("generated-request-id"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> requestIdInsideChain = new AtomicReference<>();

		filter.doFilter(request, response, (servletRequest, servletResponse) ->
				requestIdInsideChain.set(MDC.get(MdcLoggingFilter.MDC_KEY)));

		assertThat(requestIdInsideChain).hasValue("generated-request-id");
		assertThat(response.getHeader(MdcLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("generated-request-id");
		assertThat(MDC.get(MdcLoggingFilter.MDC_KEY)).isNull();
	}

	@Test
	void logsReturnedServerErrorResponse(final CapturedOutput output) throws ServletException, IOException {
		MdcLoggingFilter filter = new MdcLoggingFilter(new FixedRequestIdGenerator("request-id"));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (servletRequest, servletResponse) ->
				((MockHttpServletResponse) servletResponse).setStatus(500));

		assertThat(output).contains("HTTP server error method=GET uri=/test status=500");
	}

	@Test
	void logsAndRethrowsExceptionFromDownstreamFilter(final CapturedOutput output) {
		MdcLoggingFilter filter = new MdcLoggingFilter(new FixedRequestIdGenerator("request-id"));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
			throw new ServletException("filter failed");
		})).isInstanceOf(ServletException.class).hasMessage("filter failed");

		assertThat(output).contains("HTTP request failed method=GET uri=/test status=500");
		assertThat(MDC.get(MdcLoggingFilter.MDC_KEY)).isNull();
	}

	@Test
	void doesNotLogReturnedHealthServerError(final CapturedOutput output) throws ServletException, IOException {
		MdcLoggingFilter filter = new MdcLoggingFilter(new FixedRequestIdGenerator("request-id"));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (servletRequest, servletResponse) ->
				((MockHttpServletResponse) servletResponse).setStatus(503));

		assertThat(output).doesNotContain("HTTP server error");
	}

	private static class FixedRequestIdGenerator extends RequestIdGenerator {

		private final String requestId;

		private FixedRequestIdGenerator(final String requestId) {
			this.requestId = requestId;
		}

		@Override
		public String generate() {
			return requestId;
		}
	}
}
