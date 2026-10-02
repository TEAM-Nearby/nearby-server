// 동행 신고 이메일 제목과 본문을 생성하는 포맷터
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import com.sopt.nearby.companion.port.out.CompanionReportMail;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class CompanionReportMailFormatter {

	private static final int SUBJECT_CONTENT_LENGTH = 20;
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.KOREA);
	private static final DateTimeFormatter CITY_DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.KOREA);

	private CompanionReportMailFormatter() {
	}

	public static String subject(final CompanionReportMail mail) {
		String content = normalize(mail.companionPostContent());
		if (content.isBlank()) {
			return "동행 신고 보고서";
		}
		int codePointCount = content.codePointCount(0, content.length());
		if (codePointCount <= SUBJECT_CONTENT_LENGTH) {
			return content + " 신고 보고서";
		}
		int endIndex = content.offsetByCodePoints(0, SUBJECT_CONTENT_LENGTH);
		return content.substring(0, endIndex) + "... 신고 보고서";
	}

	public static String body(final CompanionReportMail mail) {
		String placeTime = formatPlaceDateTime(mail.scheduledAt(), mail.placeAddress());
		String reasons = mail.reasons().stream()
				.map(CompanionReportMailFormatter::reasonLabel)
				.reduce((left, right) -> left + ", " + right)
				.orElse("미입력");

		return "신고 정보\n"
				+ "- 신고 ID: " + mail.report().id() + "\n"
				+ "- 신고 시간: " + formatUtcLocalDateTime(mail.report().createdAt(), KOREA_ZONE) + "\n\n"
				+ "신고한 사용자\n"
				+ "- 유저 ID: " + mail.report().reporterUserId() + "\n"
				+ "- 닉네임: " + valueOrUnregistered(mail.reporterNickname()) + "\n"
				+ "- 유저 전화번호: " + valueOrUnregistered(mail.reporterPhoneNumber()) + "\n\n"
				+ "신고 당한 사용자\n"
				+ "- 유저 ID: " + mail.report().reportedUserId() + "\n"
				+ "- 닉네임: " + valueOrUnregistered(mail.reportedNickname()) + "\n"
				+ "- 유저 전화번호: " + valueOrUnregistered(mail.reportedPhoneNumber()) + "\n\n"
				+ "신고 동행 정보\n"
				+ "- 동행글 ID: " + mail.companionPostId() + "\n"
				+ "- 동행 제목: " + valueOrUnregistered(normalize(mail.companionPostContent())) + "\n"
				+ "- 동행 예정 일시: " + placeTime + "\n\n"
				+ "신고 내용\n"
				+ "- 신고 사유: " + reasons + "\n"
				+ "- 상세 내용: " + valueOrUnregistered(mail.report().detail());
	}

	public static String htmlBody(final CompanionReportMail mail) {
		String placeTime = formatPlaceDateTime(mail.scheduledAt(), mail.placeAddress());
		String reasons = mail.reasons().stream()
				.map(CompanionReportMailFormatter::reasonLabel)
				.reduce((left, right) -> left + ", " + right)
				.orElse("미입력");

		return """
				<div style="font-family: Arial, 'Noto Sans KR', sans-serif; line-height: 1.6; color: #222;">
				  <h2 style="font-size: 20px; font-weight: 700; margin: 0 0 8px;">신고 정보</h2>
				  <p><strong>신고 ID:</strong> %s<br>
				     <strong>신고 시간:</strong> %s</p>

				  <h2 style="font-size: 20px; font-weight: 700; margin: 24px 0 8px;">신고한 사용자</h2>
				  <p><strong>유저 ID:</strong> %s<br>
				     <strong>닉네임:</strong> %s<br>
				     <strong>유저 전화번호:</strong> %s</p>

				  <h2 style="font-size: 20px; font-weight: 700; margin: 24px 0 8px;">신고 당한 사용자</h2>
				  <p><strong>유저 ID:</strong> %s<br>
				     <strong>닉네임:</strong> %s<br>
				     <strong>유저 전화번호:</strong> %s</p>

				  <h2 style="font-size: 20px; font-weight: 700; margin: 24px 0 8px;">신고 동행 정보</h2>
				  <p><strong>동행글 ID:</strong> %s<br>
				     <strong>동행 제목:</strong> %s<br>
				     <strong>동행 예정 일시:</strong> %s</p>

				  <h2 style="font-size: 20px; font-weight: 700; margin: 24px 0 8px;">신고 내용</h2>
				  <p><strong>신고 사유:</strong> %s<br>
				     <strong>상세 내용:</strong> %s</p>
				</div>
				""".formatted(
				escapeHtml(mail.report().id()),
				escapeHtml(formatUtcLocalDateTime(mail.report().createdAt(), KOREA_ZONE)),
				escapeHtml(mail.report().reporterUserId()),
				escapeHtml(valueOrUnregistered(mail.reporterNickname())),
				escapeHtml(valueOrUnregistered(mail.reporterPhoneNumber())),
				escapeHtml(mail.report().reportedUserId()),
				escapeHtml(valueOrUnregistered(mail.reportedNickname())),
				escapeHtml(valueOrUnregistered(mail.reportedPhoneNumber())),
				escapeHtml(mail.companionPostId()),
				escapeHtml(valueOrUnregistered(normalize(mail.companionPostContent()))),
				escapeHtml(placeTime),
				escapeHtml(reasons),
				escapeHtml(valueOrUnregistered(mail.report().detail()))
		);
	}

	private static String normalize(final String value) {
		return value == null ? "" : value.replaceAll("\\s+", " ").trim();
	}

	private static String formatUtcLocalDateTime(
			final java.time.LocalDateTime value,
			final ZoneId zoneId
	) {
		return formatUtcLocalDateTime(value, zoneId, DATE_TIME_FORMATTER);
	}

	private static String formatPlaceDateTime(
			final java.time.LocalDateTime value,
			final String placeAddress
	) {
		ZoneId placeZone = CompanionPlaceCityNameResolver.resolveSupportedCity(placeAddress)
				.map(CompanionCity::zoneId)
				.orElse(ZoneOffset.UTC);
		String cityName = CompanionPlaceCityNameResolver.resolveSupportedCity(placeAddress)
				.map(CompanionCity::displayName)
				.orElse("도시 미정");
		return formatUtcLocalDateTime(value, placeZone, CITY_DATE_TIME_FORMATTER)
				+ " (" + cityName + ")";
	}

	private static String formatUtcLocalDateTime(
			final java.time.LocalDateTime value,
			final ZoneId zoneId,
			final DateTimeFormatter formatter
	) {
		if (value == null) {
			return "미등록";
		}
		ZonedDateTime zonedDateTime = value.atOffset(ZoneOffset.UTC).atZoneSameInstant(zoneId);
		return zonedDateTime.format(formatter);
	}

	private static String reasonLabel(final ReportReason reason) {
		return switch (reason) {
			case BAD_ATTITUDE -> "부적절한 언행이나 태도를 했어요";
			case NO_SHOW -> "약속 장소에 나타나지 않았어요 (노쇼)";
			case SAFETY_THREAT -> "안전을 위협하거나 위험행동을 했어요";
			case MONEY_REQUEST -> "금전 요구를 했어요";
			case ETC -> "기타";
		};
	}

	private static String valueOrUnregistered(final String value) {
		return value == null || value.isBlank() ? "미등록" : value;
	}

	private static String escapeHtml(final Object value) {
		return String.valueOf(value)
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
