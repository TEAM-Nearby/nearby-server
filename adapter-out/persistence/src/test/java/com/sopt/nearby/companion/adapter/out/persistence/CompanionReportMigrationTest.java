// 신고 매칭 이관과 만남 없는 신고의 무결성 및 중복 제약을 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class CompanionReportMigrationTest {

    @Test
    void backfillsMatchAndPreservesReportReasonsWhileEnforcingMatchUniqueness() throws SQLException {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:report242_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "")) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__create_initial_schema.sql"));
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        insert into companion_post (id, host_user_id, place_id, meeting_at, max_participants,
                            content, open_chat_url, status, created_at)
                        values (1, 7, 1, current_timestamp, 3, '본문', 'https://example.com', 'CLOSED', current_timestamp)
                        """);
                statement.executeUpdate("""
                        insert into companion_match (id, post_id, status, created_at)
                        values (10, 1, 'COMPLETED', current_timestamp), (20, 1, 'MATCHED', current_timestamp)
                        """);
                statement.executeUpdate("insert into companion_meeting (id, match_id, status) values (100, 10, 'COMPLETED')");
                statement.executeUpdate("""
                        insert into companion_report (id, meeting_id, reporter_user_id, reported_user_id, detail, created_at)
                        values (1000, 100, 7, 8, '기존 상세', current_timestamp)
                        """);
                statement.executeUpdate("insert into companion_report_reason values (1000, 'BAD_ATTITUDE')");

                ScriptUtils.executeSqlScript(connection,
                        new ClassPathResource("db/migration/V30__associate_companion_report_with_match.sql"));

                try (var result = statement.executeQuery("""
                        select r.id, r.match_id, r.meeting_id, r.detail, reason.reason
                        from companion_report r join companion_report_reason reason on reason.report_id = r.id
                        """)) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getLong("id")).isEqualTo(1000);
                    assertThat(result.getLong("match_id")).isEqualTo(10);
                    assertThat(result.getLong("meeting_id")).isEqualTo(100);
                    assertThat(result.getString("detail")).isEqualTo("기존 상세");
                    assertThat(result.getString("reason")).isEqualTo("BAD_ATTITUDE");
                    assertThat(result.next()).isFalse();
                }

                String insert = """
                        insert into companion_report (match_id, meeting_id, reporter_user_id, reported_user_id, created_at)
                        values %s
                        """;
                statement.executeUpdate(insert.formatted("(20, null, 7, 8, current_timestamp)"));
                statement.executeUpdate(insert.formatted("(20, null, 7, 9, current_timestamp)"));
                statement.executeUpdate("insert into companion_meeting (id, match_id, status) values (200, 20, 'ONGOING')");
                for (String values : new String[] {
                        "(10, null, 7, 8, current_timestamp)",
                        "(20, null, 7, 8, current_timestamp)",
                        "(20, 200, 7, 8, current_timestamp)",
                        "(null, null, 7, 8, current_timestamp)",
                        "(999, null, 7, 8, current_timestamp)"
                }) {
                    assertThatThrownBy(() -> statement.executeUpdate(insert.formatted(values)))
                            .isInstanceOf(SQLException.class);
                }
            }
        }
    }
}
