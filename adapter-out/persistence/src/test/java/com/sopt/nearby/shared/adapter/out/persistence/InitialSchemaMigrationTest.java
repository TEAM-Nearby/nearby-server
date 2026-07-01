// 초기 PostgreSQL 스키마 마이그레이션 SQL을 검증하는 테스트
package com.sopt.nearby.shared.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class InitialSchemaMigrationTest {

	@Test
	void initialMigrationCreatesMappedTables() throws SQLException {
		ClassPathResource migration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");

		assertThat(migration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, migration);

			assertThat(tableNames(connection))
					.contains(
							"user_account",
							"place_cache",
							"companion_profile",
							"companion_post",
							"companion_application",
							"companion_match",
							"companion_meeting",
							"companion_report",
							"companion_review",
							"event_publication",
							"event_publication_archive"
					);
		}
	}

	private static List<String> tableNames(final Connection connection) throws SQLException {
		List<String> names = new ArrayList<>();
		try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
			while (tables.next()) {
				names.add(tables.getString("TABLE_NAME").toLowerCase());
			}
		}
		return names;
	}
}
