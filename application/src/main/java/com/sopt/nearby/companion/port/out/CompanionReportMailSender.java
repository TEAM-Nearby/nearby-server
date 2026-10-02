// 동행 신고 이메일 발송 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

public interface CompanionReportMailSender {

	void send(CompanionReportMail mail);
}
