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

	@Test
	void secondMigrationAddsAuthUniquenessConstraints() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource authMigration = new ClassPathResource("db/migration/V2__add_auth_unique_constraints.sql");

		assertThat(authMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_auth_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, authMigration);

			assertThat(indexNames(connection, "social_account"))
					.anyMatch(indexName -> indexName.startsWith("uk_social_account_provider_user"));
			assertThat(indexNames(connection, "refresh_token"))
					.anyMatch(indexName -> indexName.startsWith("uk_refresh_token_hash"));
		}
	}

	@Test
	void thirdMigrationAddsPhoneVerificationCodeHash() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource authMigration = new ClassPathResource("db/migration/V2__add_auth_unique_constraints.sql");
		ClassPathResource phoneVerificationMigration = new ClassPathResource(
				"db/migration/V3__add_phone_verification_code_hash.sql"
		);

		assertThat(phoneVerificationMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_phone_verification_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, authMigration);
			ScriptUtils.executeSqlScript(connection, phoneVerificationMigration);

			assertThat(columnNames(connection, "phone_verification"))
					.contains("verification_code_hash");
		}
	}

	@Test
	void fourthMigrationAddsPlaceCacheGooglePlaceIdUniquenessConstraint() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource authMigration = new ClassPathResource("db/migration/V2__add_auth_unique_constraints.sql");
		ClassPathResource phoneVerificationMigration = new ClassPathResource(
				"db/migration/V3__add_phone_verification_code_hash.sql"
		);
		ClassPathResource placeCacheMigration = new ClassPathResource(
				"db/migration/V4__add_place_cache_google_place_id_unique_constraint.sql"
		);

		assertThat(placeCacheMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_place_cache_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, authMigration);
			ScriptUtils.executeSqlScript(connection, phoneVerificationMigration);
			ScriptUtils.executeSqlScript(connection, placeCacheMigration);

			assertThat(indexNames(connection, "place_cache"))
					.anyMatch(indexName -> indexName.startsWith("uk_place_cache_google_place_id"));
		}
	}

	@Test
	void fifthMigrationAddsCompanionProfileUniquenessConstraints() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource authMigration = new ClassPathResource("db/migration/V2__add_auth_unique_constraints.sql");
		ClassPathResource phoneVerificationMigration = new ClassPathResource(
				"db/migration/V3__add_phone_verification_code_hash.sql"
		);
		ClassPathResource placeCacheMigration = new ClassPathResource(
				"db/migration/V4__add_place_cache_google_place_id_unique_constraint.sql"
		);
		ClassPathResource companionProfileMigration = new ClassPathResource(
				"db/migration/V5__add_companion_profile_unique_constraints.sql"
		);

		assertThat(companionProfileMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_companion_profile_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, authMigration);
			ScriptUtils.executeSqlScript(connection, phoneVerificationMigration);
			ScriptUtils.executeSqlScript(connection, placeCacheMigration);
			ScriptUtils.executeSqlScript(connection, companionProfileMigration);

			assertThat(indexNames(connection, "companion_profile"))
					.anyMatch(indexName -> indexName.startsWith("uk_companion_profile_nickname"));
			assertThat(indexNames(connection, "companion_profile"))
					.anyMatch(indexName -> indexName.startsWith("uk_companion_profile_user"));
		}
	}

	@Test
	void tenthMigrationAddsCompanionPostCreationFields() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource companionPostMigration = new ClassPathResource(
				"db/migration/V10__add_companion_post_creation_fields.sql"
		);

		assertThat(companionPostMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_companion_post_creation_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, companionPostMigration);

			assertThat(columnNames(connection, "companion_post"))
					.contains("meeting_time_type", "exposure_expires_at", "depart_even_if_not_full");
		}
	}

	@Test
	void eleventhMigrationAddsMeetingCheckInUniquenessConstraint() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource meetingCheckInMigration = new ClassPathResource(
				"db/migration/V11__add_meeting_check_in_meeting_user_unique_constraint.sql"
		);

		assertThat(meetingCheckInMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_meeting_check_in_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, meetingCheckInMigration);

			assertThat(indexNames(connection, "meeting_check_in"))
					.anyMatch(indexName -> indexName.startsWith("uk_meeting_check_in_meeting_user"));
		}
	}

	@Test
	void thirteenthMigrationAddsCompanionReviewUniquenessConstraint() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource companionReviewMigration = new ClassPathResource(
				"db/migration/V13__add_companion_review_meeting_reviewer_reviewee_unique_constraint.sql"
		);

		assertThat(companionReviewMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_companion_review_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, companionReviewMigration);

			assertThat(indexNames(connection, "companion_review"))
					.anyMatch(indexName -> indexName.startsWith("uk_companion_review_meeting_reviewer_reviewee"));
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

	private static List<String> indexNames(final Connection connection, final String tableName) throws SQLException {
		List<String> names = new ArrayList<>();
		try (ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
			while (indexes.next()) {
				String indexName = indexes.getString("INDEX_NAME");
				if (indexName != null) {
					names.add(indexName.toLowerCase());
				}
			}
		}
		return names;
	}

	private static List<String> columnNames(final Connection connection, final String tableName) throws SQLException {
		List<String> names = new ArrayList<>();
		try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, null)) {
			while (columns.next()) {
				names.add(columns.getString("COLUMN_NAME").toLowerCase());
			}
		}
		return names;
	}
}
