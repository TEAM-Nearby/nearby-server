// 동행 신고 이메일 제목과 본문 포맷을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import com.sopt.nearby.companion.port.out.CompanionReportMail;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompanionReportMailFormatterTest {

	@Test
	void truncatesSubjectToTwentyCharactersAfterNormalizingWhitespace() {
		CompanionReportMail mail = mail("12345678901234567890abc\ndef");

		assertEquals(
				"12345678901234567890... 신고 보고서",
				CompanionReportMailFormatter.subject(mail)
		);
	}

	@Test
	void usesFallbackSubjectWhenPostContentIsBlank() {
		CompanionReportMail mail = mail(" \n ");

		assertEquals("동행 신고 보고서", CompanionReportMailFormatter.subject(mail));
	}

	@Test
	void formatsReportTimeInKoreaTime() {
		CompanionReportMail mail = mail("동행 본문");

		assertTrue(CompanionReportMailFormatter.body(mail).contains("2026-10-02 09:00:00 KST"));
	}

	@Test
	void formatsScheduledTimeWithCityNameInsteadOfTimezone() {
		CompanionReportMail mail = mail("동행 본문");

		assertTrue(CompanionReportMailFormatter.body(mail).contains("2026-10-02 02:00:00 (Paris)"));
	}

	@Test
	void usesUndecidedCityWhenAddressDoesNotContainSupportedCity() {
		CompanionReportMail mail = mail("동행 본문", "Rambla de Catalunya, 16");

		assertTrue(CompanionReportMailFormatter.body(mail).contains("(도시 미정)"));
	}

	@Test
	void formatsHtmlSectionHeadingsAsBoldAndLargerText() {
		CompanionReportMail mail = mail("동행 본문");

		String html = CompanionReportMailFormatter.htmlBody(mail);

		assertTrue(html.contains("<h2 style=\"font-size: 20px; font-weight: 700;"));
		assertTrue(html.contains("<strong>신고 ID:</strong>"));
	}

	private CompanionReportMail mail(final String content) {
		return mail(content, "Paris, France");
	}

	private CompanionReportMail mail(final String content, final String placeAddress) {
		return new CompanionReportMail(
				new CompanionReport(
						1L,
						2L,
						3L,
						4L,
						"상세 내용",
						LocalDateTime.of(2026, 10, 2, 0, 0)
				),
				List.of(ReportReason.BAD_ATTITUDE),
				"신고자",
				"01012345678",
				"피신고자",
				null,
				5L,
				content,
				LocalDateTime.of(2026, 10, 2, 0, 0),
				placeAddress
		);
	}
}
