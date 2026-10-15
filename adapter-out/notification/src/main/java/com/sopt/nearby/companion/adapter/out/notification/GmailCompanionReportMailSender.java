// Gmail SMTP로 동행 신고 보고서를 발송하는 어댑터
package com.sopt.nearby.companion.adapter.out.notification;

import com.sopt.nearby.companion.application.CompanionReportMailFormatter;
import com.sopt.nearby.companion.port.out.CompanionReportMail;
import com.sopt.nearby.companion.port.out.CompanionReportMailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class GmailCompanionReportMailSender implements CompanionReportMailSender {

	private static final Logger log = LoggerFactory.getLogger(GmailCompanionReportMailSender.class);

	private final JavaMailSender mailSender;
	private final String from;
	private final String to;

	public GmailCompanionReportMailSender(
			final JavaMailSender mailSender,
			@Value("${spring.mail.username:}") final String from,
			@Value("${nearby.report.mail.to:nearbytravel11@gmail.com}") final String to
	) {
		this.mailSender = mailSender;
		this.from = from;
		this.to = to;
	}

	@Override
	public void send(final CompanionReportMail mail) {
		if (from.isBlank() || to.isBlank()) {
			log.warn("동행 신고 이메일 설정이 없어 발송을 건너뛰었습니다. reportId={}", mail.report().id());
			return;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					true,
					StandardCharsets.UTF_8.name()
			);
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(CompanionReportMailFormatter.subject(mail));
			helper.setText(
					CompanionReportMailFormatter.body(mail),
					CompanionReportMailFormatter.htmlBody(mail)
			);
			mailSender.send(message);
		} catch (MessagingException | MailException exception) {
			log.warn("동행 신고 이메일 발송에 실패했습니다. reportId={}", mail.report().id(), exception);
		}
	}
}
