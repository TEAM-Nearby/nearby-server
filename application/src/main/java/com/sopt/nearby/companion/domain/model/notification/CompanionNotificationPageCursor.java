// 동행 알림 페이지 조회의 다음 위치를 표현하고 검증한다.
package com.sopt.nearby.companion.domain.model.notification;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionNotificationCursorException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public record CompanionNotificationPageCursor(
        int version,
        CompanionNotificationDirection direction,
        LocalDateTime createdAt,
        Long notificationId
) {

    public static final int CURRENT_VERSION = 1;

    public CompanionNotificationPageCursor {
        if (version != CURRENT_VERSION || direction == null || createdAt == null
                || notificationId == null || notificationId <= 0) {
            throw new InvalidCompanionNotificationCursorException();
        }
    }

    public static CompanionNotificationPageCursor of(
            final CompanionNotificationDirection direction,
            final LocalDateTime createdAt,
            final Long notificationId
    ) {
        return new CompanionNotificationPageCursor(CURRENT_VERSION, direction, createdAt, notificationId);
    }

    public String encode() {
        String plain = version + "|" + direction.name() + "|" + createdAt + "|" + notificationId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    public static CompanionNotificationPageCursor decode(final String encoded) {
        if (encoded == null || encoded.isBlank() || encoded.length() > 512) {
            throw new InvalidCompanionNotificationCursorException();
        }
        try {
            String plain = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = plain.split("\\|", -1);
            if (parts.length != 4) {
                throw new InvalidCompanionNotificationCursorException();
            }
            return new CompanionNotificationPageCursor(
                    Integer.parseInt(parts[0]),
                    CompanionNotificationDirection.valueOf(parts[1]),
                    LocalDateTime.parse(parts[2]),
                    Long.parseLong(parts[3])
            );
        } catch (RuntimeException exception) {
            if (exception instanceof InvalidCompanionNotificationCursorException cursorException) {
                throw cursorException;
            }
            throw new InvalidCompanionNotificationCursorException();
        }
    }
}
