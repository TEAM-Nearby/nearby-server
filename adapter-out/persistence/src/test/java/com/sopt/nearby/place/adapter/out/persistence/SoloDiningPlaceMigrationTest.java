// 혼밥 등록 목록의 원본 기반 이관과 공용 장소 참조 보존을 검증한다.
package com.sopt.nearby.place.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class SoloDiningPlaceMigrationTest {

    @Test
    void registersOnlyConfirmedPlacesAndPreservesExistingReferences() throws SQLException {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:solo_dining_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "")) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__create_initial_schema.sql"));
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        insert into place_cache (id, google_place_id, name, latitude, longitude, category, business_status)
                        values (1, 'ChIJAQDcNqxv5kcRP3g3xJfp2sc', '파리 원본 장소', 48.86, 2.34, 'RESTAURANT', 'UNKNOWN'),
                               (2, 'ChIJ_YLEDtUadkgRq4gE6-fNw10', '런던 원본 장소', 51.51, -0.13, 'CAFE', 'OPERATIONAL'),
                               (3, 'companion-only', '동행 장소', 48.86, 2.34, 'RESTAURANT', 'OPERATIONAL')
                        """);
                statement.executeUpdate("""
                        insert into solo_dining_favorite (id, user_id, place_id, created_at)
                        values (1, 7, 1, timestamp '2026-10-17 00:00:00'),
                               (2, 7, 3, timestamp '2026-10-17 00:00:00')
                        """);
                statement.executeUpdate("""
                        insert into companion_post (id, host_user_id, place_id, meeting_at, max_participants,
                            content, open_chat_url, status, created_at)
                        values (1, 7, 1, timestamp '2026-10-18 00:00:00', 2, '식사해요',
                            'https://open.kakao.com/o/test', 'RECRUITING', timestamp '2026-10-17 00:00:00'),
                               (2, 7, 3, timestamp '2026-10-18 00:00:00', 2, '식사해요',
                            'https://open.kakao.com/o/test', 'RECRUITING', timestamp '2026-10-17 00:00:00')
                        """);

                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V29__create_solo_dining_place.sql"));

                assertThat(ids(connection, "select place_id from solo_dining_place order by place_id"))
                        .containsExactly(1L, 2L);
                assertThat(ids(connection, "select id from place_cache order by id")).containsExactly(1L, 2L, 3L);
                assertThat(ids(connection, "select place_id from companion_post order by id")).containsExactly(1L, 3L);
                assertThat(ids(connection, "select place_id from solo_dining_favorite order by id")).containsExactly(1L, 3L);

                statement.executeUpdate("""
                        insert into place_cache (id, google_place_id, name, latitude, longitude, category, business_status)
                        values (4, 'new-companion', '새 동행 장소', 48.86, 2.34, 'RESTAURANT', 'UNKNOWN')
                        """);
                assertThat(ids(connection, "select place_id from solo_dining_place order by place_id"))
                        .containsExactly(1L, 2L);

                for (long id : new long[]{1, 999}) {
                    assertThatThrownBy(() -> statement.executeUpdate("insert into solo_dining_place values (" + id + ")"))
                            .isInstanceOf(SQLException.class);
                }
                statement.executeUpdate("insert into solo_dining_place values (4)");
                statement.executeUpdate("delete from place_cache where id = 4");
                assertThat(ids(connection, "select place_id from solo_dining_place order by place_id"))
                        .containsExactly(1L, 2L);
            }
        }
    }

    private List<Long> ids(final Connection connection, final String sql) throws SQLException {
        List<Long> result = new ArrayList<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                result.add(rows.getLong(1));
            }
        }
        return result;
    }
}
