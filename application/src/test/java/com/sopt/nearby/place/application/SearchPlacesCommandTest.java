// 장소 검색의 정규화와 입력 경계를 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.place.domain.exception.InvalidPlaceSearchRequestException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SearchPlacesCommandTest {
    @Test
    void normalizesTextAndLanguageWithoutTranslatingPlaceNames() {
        SearchPlacesCommand command = command("  시우다드   콘달  ", " KO ", 20_000, 20);
        assertEquals("시우다드 콘달", command.query());
        assertEquals("ko", command.languageCode());
        assertEquals("Ciutat Comtal", command("Ciutat Comtal", "es", 1, 1).query());
        assertEquals("ko", command("a".repeat(200), null, 50_000, 20).languageCode());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsMissingQuery(final String query) {
        assertThrows(InvalidPlaceSearchRequestException.class, () -> command(query, "ko", 1000, 20));
    }

    @Test
    void rejectsInvalidBoundsAndLanguage() {
        assertThrows(InvalidPlaceSearchRequestException.class, () -> command("a".repeat(201), "ko", 1000, 20));
        for (String language : new String[]{"", "KR", "xx"}) {
            assertThrows(InvalidPlaceSearchRequestException.class, () -> command("장소", language, 1000, 20));
        }
        for (int radius : new int[]{0, -1, 50_001}) {
            assertThrows(InvalidPlaceSearchRequestException.class, () -> command("장소", "ko", radius, 20));
        }
        for (int size : new int[]{0, -1, 21}) {
            assertThrows(InvalidPlaceSearchRequestException.class, () -> command("장소", "ko", 1000, size));
        }
        for (BigDecimal latitude : new BigDecimal[]{null, new BigDecimal("90.01"), new BigDecimal("-90.01")}) {
            assertThrows(InvalidPlaceSearchRequestException.class, () -> new SearchPlacesCommand(
                    "장소", latitude, BigDecimal.ZERO, 1000, "ko", 20, null));
        }
        assertThrows(InvalidPlaceSearchRequestException.class, () -> new SearchPlacesCommand(
                "장소", BigDecimal.ZERO, new BigDecimal("180.01"), 1000, "ko", 20, null));
        assertThrows(InvalidPlaceSearchRequestException.class, () -> new SearchPlacesCommand(
                "장소", BigDecimal.ZERO, BigDecimal.ZERO, 1000, "ko", 20, "x".repeat(4097)));
    }

    private SearchPlacesCommand command(final String query, final String language, final int radius, final int size) {
        return new SearchPlacesCommand(query, new BigDecimal("41.3874"), new BigDecimal("2.1686"),
                radius, language, size, null);
    }
}
