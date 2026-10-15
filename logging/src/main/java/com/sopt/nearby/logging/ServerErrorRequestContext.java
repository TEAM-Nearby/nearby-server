// HTTP 5xx의 원인 예외를 요청 범위에서 전달하는 컨텍스트
package com.sopt.nearby.logging;

import jakarta.servlet.http.HttpServletRequest;

public final class ServerErrorRequestContext {

	private static final String EXCEPTION_ATTRIBUTE =
			ServerErrorRequestContext.class.getName() + ".exception";

	private ServerErrorRequestContext() {
	}

	public static void record(final HttpServletRequest request, final Throwable throwable) {
		if (request.getAttribute(EXCEPTION_ATTRIBUTE) == null) {
			request.setAttribute(EXCEPTION_ATTRIBUTE, throwable);
		}
	}

	static Throwable find(final HttpServletRequest request) {
		Object attribute = request.getAttribute(EXCEPTION_ATTRIBUTE);
		if (attribute instanceof Throwable throwable) {
			return throwable;
		}
		return null;
	}
}
