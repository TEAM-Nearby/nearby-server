// SOLAPI SDK로 휴대폰 인증 문자를 실제 발송하는 어댑터
package com.sopt.nearby.user.adapter.out.notification;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiEmptyResponseException;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.exception.SolapiUnknownException;
import com.solapi.sdk.message.model.Message;
import com.sopt.nearby.user.exception.PhoneVerificationSendLimitExceededException;
import com.sopt.nearby.user.port.out.PhoneVerificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("solapi")
public class SolapiPhoneVerificationSender implements PhoneVerificationSender {

	private static final Logger log = LoggerFactory.getLogger(SolapiPhoneVerificationSender.class);
	private static final String MESSAGE_TEMPLATE = "[Nearby] 인증번호는 %s입니다.";

	private final String fromNumber;
	private final SolapiMessageClient messageClient;

	@Autowired
	public SolapiPhoneVerificationSender(
			@Value("${nearby.sms.solapi.api-key}") final String apiKey,
			@Value("${nearby.sms.solapi.api-secret}") final String apiSecret,
			@Value("${nearby.sms.solapi.from-number}") final String fromNumber
	) {
		this(fromNumber, SolapiClient.INSTANCE.createInstance(required(apiKey, "SOLAPI API key가 필요합니다."),
				required(apiSecret, "SOLAPI API secret이 필요합니다."))::send);
	}

	SolapiPhoneVerificationSender(
			final String fromNumber,
			final SolapiMessageClient messageClient
	) {
		this.fromNumber = required(fromNumber, "SOLAPI 발신번호가 필요합니다.");
		this.messageClient = messageClient;
	}

	@Override
	public void send(final String phoneNumber, final String verificationCode) {
		Message message = new Message();
		message.setFrom(fromNumber);
		message.setTo(phoneNumber);
		message.setText(MESSAGE_TEMPLATE.formatted(verificationCode));

		try {
			messageClient.send(message);
		} catch (SolapiMessageNotReceivedException | SolapiEmptyResponseException | SolapiUnknownException exception) {
			log.warn("SOLAPI 휴대폰 인증 문자 발송에 실패했습니다. phoneNumber={}", mask(phoneNumber), exception);
			throw new PhoneVerificationSendLimitExceededException();
		}
	}

	private static String required(final String value, final String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

	private String mask(final String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() < 7) {
			return "***";
		}
		return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
	}

	@FunctionalInterface
	interface SolapiMessageClient {

		void send(Message message)
				throws SolapiMessageNotReceivedException, SolapiEmptyResponseException, SolapiUnknownException;
	}
}
