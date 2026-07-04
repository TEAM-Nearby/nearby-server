// 인증 실패를 공통 JSON 응답 형태로 변환하는 Spring Security 진입점
package com.sopt.nearby.security.adapter.out;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final String UNAUTHORIZED_RESPONSE = """
			{"status":401,"code":"UNAUTHORIZED","message":"인증이 필요합니다.","data":null}
			""";

	@Override
	public void commence(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final AuthenticationException authException
	) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(UNAUTHORIZED_RESPONSE);
	}
}
