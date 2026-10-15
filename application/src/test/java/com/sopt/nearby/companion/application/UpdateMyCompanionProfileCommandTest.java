// 프로필 수정 입력의 경계값과 선택 정보 삭제 규칙을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionProfileUpdateException;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpdateMyCompanionProfileCommandTest {
    private static final List<TravelStyleKeyword> STYLES = List.of(TravelStyleKeyword.EXTROVERTED);

    @Test
    void acceptsBoundaryLengthsAndDefensivelyCopiesKeywords() {
        var styles = new ArrayList<>(STYLES);
        var command = new UpdateMyCompanionProfileCommand(1L, "가".repeat(15), "소".repeat(50),
                "https://example.com/" + "a".repeat(235), styles);
        styles.clear();
        assertEquals(255, command.profileImageUrl().length());
        assertEquals(STYLES, command.travelStyleKeywords());
        assertThrows(UnsupportedOperationException.class, () -> command.travelStyleKeywords().clear());
    }

    @Test
    void trimsTextAndClearsNullEmptyOrWhitespaceOptionalFields() {
        var normalized = new UpdateMyCompanionProfileCommand(1L, " 여행친구 ", " 소개 ",
                " https://example.com/image.png ", STYLES);
        assertEquals("여행친구", normalized.nickname());
        assertEquals("소개", normalized.intro());
        assertEquals("https://example.com/image.png", normalized.profileImageUrl());
        for (String empty : Arrays.asList(null, "", " \t ")) {
            var cleared = new UpdateMyCompanionProfileCommand(1L, "친구", empty, empty, STYLES);
            assertNull(cleared.intro());
            assertNull(cleared.profileImageUrl());
        }
    }

    @Test
    void rejectsMissingIdentityAndInvalidNicknameOrIntro() {
        for (Long id : Arrays.asList(null, 0L, -1L)) {
            assertThrows(InvalidCompanionProfileUpdateException.class,
                    () -> new UpdateMyCompanionProfileCommand(id, "친구", null, null, STYLES));
        }
        for (String nickname : Arrays.asList(null, "", " \t ", "가".repeat(16))) {
            assertThrows(InvalidCompanionProfileUpdateException.class,
                    () -> new UpdateMyCompanionProfileCommand(1L, nickname, null, null, STYLES));
        }
        assertThrows(InvalidCompanionProfileUpdateException.class,
                () -> new UpdateMyCompanionProfileCommand(1L, "친구", "소".repeat(51), null, STYLES));
    }

    @Test
    void rejectsMalformedNonHttpOrOverlongImageUrls() {
        for (String url : List.of("invalid", "javascript:alert(1)", "file:///tmp/image.png", "https:///image.png",
                "https://user:pass@example.com/image.png", "https://example.com/has space.png",
                "https://example.com/" + "a".repeat(237))) {
            assertThrows(InvalidCompanionProfileUpdateException.class,
                    () -> new UpdateMyCompanionProfileCommand(1L, "친구", null, url, STYLES));
        }
    }

    @Test
    void rejectsMissingEmptyDuplicateOrNullKeywords() {
        List<List<TravelStyleKeyword>> invalid = Arrays.asList(null, List.of(),
                Arrays.asList(TravelStyleKeyword.EXTROVERTED, null),
                List.of(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.EXTROVERTED));
        for (var styles : invalid) {
            assertThrows(InvalidCompanionProfileUpdateException.class,
                    () -> new UpdateMyCompanionProfileCommand(1L, "친구", null, null, styles));
        }
    }
}
