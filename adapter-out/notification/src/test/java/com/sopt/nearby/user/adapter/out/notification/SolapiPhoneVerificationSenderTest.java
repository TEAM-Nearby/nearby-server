// SOLAPI 휴대폰 인증 문자 발송 어댑터의 메시지 구성과 예외 변환을 검증하는 테스트
package com.sopt.nearby.user.adapter.out.notification;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.sopt.nearby.user.exception.PhoneVerificationSendLimitExceededException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.MapPropertySource;

class SolapiPhoneVerificationSenderTest {

	@Test
	void sendsVerificationCodeMessageThroughSolapiClient() {
		CapturingSolapiMessageClient client = new CapturingSolapiMessageClient();
		SolapiPhoneVerificationSender sender = new SolapiPhoneVerificationSender("01099998888", client);

		sender.send("01012345678", "123456");

		Message message = client.message;
		assertEquals("01099998888", message.getFrom());
		assertEquals("01012345678", message.getTo());
		assertEquals("[Nearby] 인증번호는 123456입니다.", message.getText());
	}

	@Test
	void convertsSolapiSendFailureToOnboardingException() {
		SolapiPhoneVerificationSender sender = new SolapiPhoneVerificationSender(
				"01099998888",
				message -> {
					throw new SolapiMessageNotReceivedException("SOLAPI rejected message");
				}
		);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> sender.send("01012345678", "123456")
		);

		assertInstanceOf(PhoneVerificationSendLimitExceededException.class, exception);
	}

	@Test
	void requiresRegisteredFromNumber() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> new SolapiPhoneVerificationSender("", message -> {
				})
		);

		assertEquals("SOLAPI 발신번호가 필요합니다.", exception.getMessage());
	}

	@Test
	void separatesLocalAndSolapiSendersByProfile() {
		assertArrayEquals(
				new String[]{"solapi"},
				SolapiPhoneVerificationSender.class.getAnnotation(Profile.class).value()
		);
		assertArrayEquals(
				new String[]{"!solapi"},
				LocalPhoneVerificationSender.class.getAnnotation(Profile.class).value()
		);
	}

	@Test
	void springCreatesSolapiSenderWithConfiguredProperties() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().setActiveProfiles("solapi");
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
				"test-solapi",
				Map.of(
						"nearby.sms.solapi.api-key", "test-api-key",
						"nearby.sms.solapi.api-secret", "test-api-secret",
						"nearby.sms.solapi.from-number", "01099998888"
				)
		));
		context.register(SolapiPhoneVerificationSender.class);

		assertDoesNotThrow(context::refresh);
		context.close();
	}

	private static final class CapturingSolapiMessageClient implements SolapiPhoneVerificationSender.SolapiMessageClient {

		private Message message;

		@Override
		public void send(final Message message) {
			this.message = message;
		}
	}
}
