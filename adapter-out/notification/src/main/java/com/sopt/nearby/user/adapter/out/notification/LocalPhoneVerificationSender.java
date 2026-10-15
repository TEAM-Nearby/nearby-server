// 휴대폰 인증 문자 발송 포트를 로컬 로그 기반으로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.notification;

import com.sopt.nearby.user.port.out.PhoneVerificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!solapi")
public class LocalPhoneVerificationSender implements PhoneVerificationSender {

	private static final Logger log = LoggerFactory.getLogger(LocalPhoneVerificationSender.class);

	@Override
	public void send(final String phoneNumber, final String verificationCode) {
		// ponytail: 실제 SMS 사업자가 정해지면 이 adapter만 교체한다.
		log.info("휴대폰 인증 문자를 로컬 발송 처리했습니다. phoneNumber={}", mask(phoneNumber));
	}

	private String mask(final String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() < 7) {
			return "***";
		}
		return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
	}
}
