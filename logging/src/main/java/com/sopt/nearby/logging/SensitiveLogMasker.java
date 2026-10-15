// 로그 메시지에 포함될 수 있는 민감정보를 마스킹하는 컴포넌트
package com.sopt.nearby.logging;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SensitiveLogMasker {

	private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
			"(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+"
	);
	private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(
			"\\b(01[016789])-?(\\d{3,4})-?(\\d{4})\\b"
	);
	private static final Pattern PASSWORD_PATTERN = Pattern.compile(
			"(?i)(password\\s*[=:]\\s*)[^,\\s&]+"
	);

	public String mask(final String message) {
		if (message == null) {
			return null;
		}

		String masked = BEARER_TOKEN_PATTERN.matcher(message).replaceAll("$1***");
		masked = PHONE_NUMBER_PATTERN.matcher(masked).replaceAll("$1-****-$3");
		return PASSWORD_PATTERN.matcher(masked).replaceAll("$1***");
	}
}
