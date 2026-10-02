// Gmail 신고 메일 발송 실패가 호출자에게 전파되지 않는지 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import com.sopt.nearby.companion.port.out.CompanionReportMail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class GmailCompanionReportMailSenderTest {

	@Test
	void swallowsMailSendFailure() {
		JavaMailSender mailSender = (JavaMailSender) Proxy.newProxyInstance(
				JavaMailSender.class.getClassLoader(),
				new Class<?>[]{JavaMailSender.class},
				(proxy, method, args) -> {
					if (method.getName().equals("createMimeMessage")) {
						return new MimeMessage(Session.getInstance(new Properties()));
					}
					if (method.getName().startsWith("send")) {
						throw new MailSendException("SMTP failure");
					}
					return null;
				}
		);
		GmailCompanionReportMailSender sender = new GmailCompanionReportMailSender(
				mailSender,
				"sender@example.com",
				"receiver@example.com"
		);

		assertDoesNotThrow(() -> sender.send(mail()));
	}

	private CompanionReportMail mail() {
		return new CompanionReportMail(
				new CompanionReport(1L, 2L, 10L, 11L, "상세 내용", LocalDateTime.of(2026, 10, 2, 0, 0)),
				List.of(ReportReason.BAD_ATTITUDE),
				"신고자",
				null,
				"피신고자",
				null,
				3L,
				"동행 본문",
				LocalDateTime.of(2026, 10, 2, 0, 0),
				CompanionPostMeetingTimeType.SCHEDULED,
				"Paris, France"
		);
	}
}
