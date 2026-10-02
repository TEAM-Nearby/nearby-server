// 본인 동행 프로필의 수정 가능한 입력을 검증하고 정규화한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionProfileUpdateException;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.net.URI;
import java.util.HashSet;
import java.util.List;

public record UpdateMyCompanionProfileCommand(
        Long userId, String nickname, String intro, String profileImageUrl,
        List<TravelStyleKeyword> travelStyleKeywords
) {
    public UpdateMyCompanionProfileCommand {
        nickname = nickname == null ? null : nickname.strip();
        intro = emptyToNull(intro);
        profileImageUrl = emptyToNull(profileImageUrl);
        if (userId == null || userId <= 0 || nickname == null || nickname.isBlank() || nickname.length() > 15
                || (intro != null && intro.length() > 50)
                || (profileImageUrl != null && (profileImageUrl.length() > 255 || !isImageUrl(profileImageUrl)))
                || travelStyleKeywords == null || travelStyleKeywords.isEmpty()
                || travelStyleKeywords.stream().anyMatch(keyword -> keyword == null)
                || new HashSet<>(travelStyleKeywords).size() != travelStyleKeywords.size()) {
            throw new InvalidCompanionProfileUpdateException();
        }
        travelStyleKeywords = List.copyOf(travelStyleKeywords);
    }

    private static String emptyToNull(final String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static boolean isImageUrl(final String value) {
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
