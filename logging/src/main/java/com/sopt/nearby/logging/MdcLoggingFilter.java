// 요청 식별자를 MDC에 저장하고 요청 로그를 남기는 서블릿 필터
package com.sopt.nearby.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";
	public static final String MDC_KEY = "requestId";

	private static final Logger log = LoggerFactory.getLogger(MdcLoggingFilter.class);

	private final RequestIdGenerator requestIdGenerator;
	private final SensitiveLogMasker sensitiveLogMasker;

	@Autowired
	public MdcLoggingFilter(
			final RequestIdGenerator requestIdGenerator,
			final SensitiveLogMasker sensitiveLogMasker
	) {
		this.requestIdGenerator = requestIdGenerator;
		this.sensitiveLogMasker = sensitiveLogMasker;
	}

	MdcLoggingFilter(final RequestIdGenerator requestIdGenerator) {
		this(requestIdGenerator, new SensitiveLogMasker());
	}

	@Override
	protected void doFilterInternal(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final FilterChain filterChain
	) throws ServletException, IOException {
		String requestId = resolveRequestId(request);
		long startedAtMillis = System.currentTimeMillis();

		MDC.put(MDC_KEY, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);

		try {
			log.info("HTTP request started method={} uri={}", request.getMethod(), maskedRequestUri(request));
			filterChain.doFilter(request, response);
		} finally {
			log.info(
					"HTTP request completed method={} uri={} status={} durationMs={}",
					request.getMethod(),
					maskedRequestUri(request),
					response.getStatus(),
					System.currentTimeMillis() - startedAtMillis
			);
			MDC.remove(MDC_KEY);
		}
	}

	private String resolveRequestId(final HttpServletRequest request) {
		String requestId = request.getHeader(REQUEST_ID_HEADER);
		if (StringUtils.hasText(requestId)) {
			return requestId;
		}
		return requestIdGenerator.generate();
	}

	private String maskedRequestUri(final HttpServletRequest request) {
		String queryString = request.getQueryString();
		String requestUri = request.getRequestURI();
		if (StringUtils.hasText(queryString)) {
			requestUri = requestUri + "?" + queryString;
		}
		return sensitiveLogMasker.mask(requestUri);
	}
}
