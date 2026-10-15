// 복수 카테고리 마이그레이션의 기존 데이터 이관과 무결성 제약을 검증한다.
package com.sopt.nearby.place.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class PlaceCategoryMigrationTest {

    @Test
    void backfillsKnownCategoriesAndEnforcesCategoryIntegrity() throws SQLException {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:place_category_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "")) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__create_initial_schema.sql"));
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        insert into place_cache (id, google_place_id, name, latitude, longitude, category, business_status)
                        values (1, 'cafe', '카페', 48.86, 2.34, 'cafe', 'OPERATIONAL'),
                               (2, 'unknown', '미분류', 48.86, 2.34, null, 'UNKNOWN'),
                               (3, 'sight', '관광지', 48.86, 2.34, 'TOURIST_ATTRACTION', 'UNKNOWN')
                        """);
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V28__create_place_category.sql"));
                try (var result = statement.executeQuery("select place_id, category, category_order from place_category")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getLong("place_id")).isEqualTo(1L);
                    assertThat(result.getString("category")).isEqualTo("CAFE");
                    assertThat(result.getInt("category_order")).isZero();
                    assertThat(result.next()).isFalse();
                }
                statement.executeUpdate("insert into place_category values (1, 'DESSERT', 1)");
                for (String values : new String[] {
                        "(1, 'DESSERT', 2)", "(1, 'PUB', 0)", "(1, 'BAKERY', 2)",
                        "(1, 'PUB', -1)", "(999, 'CAFE', 0)"
                }) {
                    assertThatThrownBy(() -> statement.executeUpdate("insert into place_category values " + values))
                            .isInstanceOf(SQLException.class);
                }
                statement.executeUpdate("delete from place_cache where id = 1");
                try (var result = statement.executeQuery("select count(*) from place_category")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isZero();
                }
            }
        }
    }
}
