// 요청 식별자를 MDC와 응답 헤더에 전파하는 필터를 검증하는 테스트
package com.sopt.nearby.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
