// 초기 PostgreSQL 스키마 마이그레이션 SQL을 검증하는 테스트
package com.sopt.nearby.shared.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
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

	@Test
	void fourteenthMigrationAddsSoloDiningFavoriteUserPlaceUniquenessConstraint() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource favoriteMigration = new ClassPathResource(
				"db/migration/V14__add_solo_dining_favorite_user_place_unique_constraint.sql"
		);

		assertThat(favoriteMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_solo_dining_favorite_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			insertDuplicateSoloDiningFavorites(connection);
			ScriptUtils.executeSqlScript(connection, favoriteMigration);

			assertThat(indexNames(connection, "solo_dining_favorite"))
					.anyMatch(indexName -> indexName.startsWith("uk_solo_dining_favorite_user_place"));
			assertThat(soloDiningFavoriteCount(connection)).isEqualTo(1);
			assertThat(remainingSoloDiningFavoriteId(connection)).isEqualTo(1L);
		}
	}

	@Test
	void fifteenthMigrationAddsCompanionApplicationPostApplicantUniquenessConstraint() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource readStatusMigration = new ClassPathResource(
				"db/migration/V7__create_companion_application_read_status.sql"
		);
		ClassPathResource notificationMigration = new ClassPathResource(
				"db/migration/V8__create_companion_notification.sql"
		);
		ClassPathResource applicationMigration = new ClassPathResource(
				"db/migration/V15__add_companion_application_post_applicant_unique_constraint.sql"
		);

		assertThat(applicationMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_companion_application_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, readStatusMigration);
			ScriptUtils.executeSqlScript(connection, notificationMigration);
			insertDuplicateCompanionApplications(connection);
			ScriptUtils.executeSqlScript(connection, applicationMigration);

			assertThat(indexNames(connection, "companion_application"))
					.anyMatch(indexName -> indexName.startsWith("uk_companion_application_post_applicant"));
			assertThat(companionApplicationCount(connection)).isEqualTo(1);
			assertThat(remainingCompanionApplicationId(connection)).isEqualTo(2L);
			assertThat(remainingMatchParticipantApplicationId(connection)).isEqualTo(2L);
			assertThat(companionApplicationReadStatusApplicationIds(connection))
					.containsExactly(2L, 2L);
			assertThat(companionNotificationTargetIds(connection))
					.containsExactly(2L, 2L);
		}
	}

	@Test
	void seventeenthMigrationExpandsPlaceCachePhotoReference() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource photoReferenceMigration = new ClassPathResource(
				"db/migration/V17__expand_place_cache_photo_reference.sql"
		);

		assertThat(photoReferenceMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_place_cache_photo_reference_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, photoReferenceMigration);

			String longPhotoReference = "places/google-place-id/photos/" + "p".repeat(300);
			try (PreparedStatement statement = connection.prepareStatement("""
					insert into place_cache (
					    google_place_id,
					    name,
					    latitude,
					    longitude,
					    photo_reference,
					    business_status
					) values (?, ?, ?, ?, ?, ?)
					""")) {
				statement.setString(1, "long-photo-reference-place-id");
				statement.setString(2, "긴 사진 식별자 테스트 장소");
				statement.setBigDecimal(3, new BigDecimal("37.55473930"));
				statement.setBigDecimal(4, new BigDecimal("126.92943910"));
				statement.setString(5, longPhotoReference);
				statement.setString(6, "OPERATIONAL");
				statement.executeUpdate();
			}

			try (Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery("""
							select photo_reference
							from place_cache
							where google_place_id = 'long-photo-reference-place-id'
							""")) {
				assertThat(resultSet.next()).isTrue();
				assertThat(resultSet.getString("photo_reference")).isEqualTo(longPhotoReference);
			}
		}
	}

	@Test
	void eighteenthMigrationAddsMeetingCheckInCompletedAt() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource completionMigration = new ClassPathResource(
				"db/migration/V18__add_meeting_check_in_completed_at.sql"
		);

		assertThat(completionMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_meeting_check_in_completion_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, completionMigration);

			assertThat(columnNames(connection, "meeting_check_in")).contains("completed_at");
		}
	}

	@Test
	void nineteenthMigrationNormalizesServerGeneratedBusinessTimestampsToKst() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource readStatusMigration = new ClassPathResource(
				"db/migration/V7__create_companion_application_read_status.sql"
		);
		ClassPathResource notificationMigration = new ClassPathResource(
				"db/migration/V8__create_companion_notification.sql"
		);
		ClassPathResource companionPostMigration = new ClassPathResource(
				"db/migration/V10__add_companion_post_creation_fields.sql"
		);
		ClassPathResource completionMigration = new ClassPathResource(
				"db/migration/V18__add_meeting_check_in_completed_at.sql"
		);
		ClassPathResource kstMigration = new ClassPathResource(
				"db/migration/V19__normalize_business_timestamps_to_kst.sql"
		);

		assertThat(kstMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_kst_timestamp_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, readStatusMigration);
			ScriptUtils.executeSqlScript(connection, notificationMigration);
			ScriptUtils.executeSqlScript(connection, companionPostMigration);
			ScriptUtils.executeSqlScript(connection, completionMigration);
			insertUtcBusinessTimestamps(connection);

			ScriptUtils.executeSqlScript(connection, kstMigration);

			assertTimestamp(connection, "select created_at from companion_post where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select exposure_expires_at from companion_post where id = 1", "2026-07-17T10:00");
			assertTimestamp(connection, "select meeting_at from companion_post where id = 2", "2026-07-17T19:00");
			assertTimestamp(connection, "select created_at from companion_post where id = 2", "2026-07-17T09:00");
			assertTimestamp(connection, "select created_at from companion_application where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select created_at from companion_match where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select scheduled_at from companion_schedule where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select scheduled_at from companion_schedule where id = 2", "2026-07-17T19:00");
			assertTimestamp(connection, "select started_at from companion_meeting where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select started_at from companion_meeting where id = 2", "2026-07-17T19:00");
			assertTimestamp(connection, "select completed_at from companion_meeting where id = 2", "2026-07-18T05:00");
			assertTimestamp(connection, "select checked_in_at from meeting_check_in where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select completed_at from meeting_check_in where id = 1", "2026-07-17T10:00");
			assertTimestamp(connection, "select created_at from companion_notification where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select read_at from companion_notification where id = 1", "2026-07-17T10:00");
			assertTimestamp(connection, "select created_at from companion_review where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select phone_verified_at from user_account where id = 1", "2026-07-17T09:00");
			assertTimestamp(connection, "select expires_at from phone_verification where id = 1", "2026-07-17T09:03");
			assertTimestamp(connection, "select verified_at from phone_verification where id = 1", "2026-07-17T09:01");
			assertTimestamp(connection, "select created_at from solo_dining_favorite where id = 1", "2026-07-17T09:00");
		}
	}

	@Test
	void twentiethMigrationRestoresServerGeneratedBusinessTimestampsToUtc() throws SQLException {
		ClassPathResource initialMigration = new ClassPathResource("db/migration/V1__create_initial_schema.sql");
		ClassPathResource readStatusMigration = new ClassPathResource(
				"db/migration/V7__create_companion_application_read_status.sql"
		);
		ClassPathResource notificationMigration = new ClassPathResource(
				"db/migration/V8__create_companion_notification.sql"
		);
		ClassPathResource companionPostMigration = new ClassPathResource(
				"db/migration/V10__add_companion_post_creation_fields.sql"
		);
		ClassPathResource completionMigration = new ClassPathResource(
				"db/migration/V18__add_meeting_check_in_completed_at.sql"
		);
		ClassPathResource kstMigration = new ClassPathResource(
				"db/migration/V19__normalize_business_timestamps_to_kst.sql"
		);
		ClassPathResource utcMigration = new ClassPathResource(
				"db/migration/V20__normalize_business_timestamps_to_utc.sql"
		);

		assertThat(utcMigration.exists()).isTrue();

		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:nearby_utc_timestamp_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
				"sa",
				""
		)) {
			ScriptUtils.executeSqlScript(connection, initialMigration);
			ScriptUtils.executeSqlScript(connection, readStatusMigration);
			ScriptUtils.executeSqlScript(connection, notificationMigration);
			ScriptUtils.executeSqlScript(connection, companionPostMigration);
			ScriptUtils.executeSqlScript(connection, completionMigration);
			insertUtcBusinessTimestamps(connection);
			ScriptUtils.executeSqlScript(connection, kstMigration);

			ScriptUtils.executeSqlScript(connection, utcMigration);

			assertTimestamp(connection, "select created_at from companion_post where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select exposure_expires_at from companion_post where id = 1", "2026-07-17T01:00");
			assertTimestamp(connection, "select meeting_at from companion_post where id = 2", "2026-07-17T19:00");
			assertTimestamp(connection, "select created_at from companion_application where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select created_at from companion_match where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select scheduled_at from companion_schedule where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select scheduled_at from companion_schedule where id = 2", "2026-07-17T19:00");
			assertTimestamp(connection, "select started_at from companion_meeting where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select started_at from companion_meeting where id = 2", "2026-07-17T19:00");
			assertTimestamp(connection, "select completed_at from companion_meeting where id = 2", "2026-07-17T20:00");
			assertTimestamp(connection, "select checked_in_at from meeting_check_in where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select completed_at from meeting_check_in where id = 1", "2026-07-17T01:00");
			assertTimestamp(connection, "select created_at from companion_notification where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select read_at from companion_notification where id = 1", "2026-07-17T01:00");
			assertTimestamp(connection, "select created_at from companion_review where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select phone_verified_at from user_account where id = 1", "2026-07-17T00:00");
			assertTimestamp(connection, "select expires_at from phone_verification where id = 1", "2026-07-17T00:03");
			assertTimestamp(connection, "select verified_at from phone_verification where id = 1", "2026-07-17T00:01");
			assertTimestamp(connection, "select created_at from solo_dining_favorite where id = 1", "2026-07-17T00:00");
		}
	}

	private static void insertDuplicateSoloDiningFavorites(final Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("""
					insert into place_cache (
					    id,
					    google_place_id,
					    name,
					    address,
					    latitude,
					    longitude,
					    category,
					    phone_number,
					    rating,
					    review_count,
					    photo_reference,
					    business_status
					) values (
					    12,
					    'google-place-id',
					    '니어바이 카페',
					    '서울특별시 중구 세종대로 110',
					    37.56612000,
					    126.97845000,
					    'CAFE',
					    null,
					    4.30,
					    22870,
					    'places/google-place-id/photos/photo-resource',
					    'OPERATIONAL'
					)
					""");
			statement.executeUpdate("""
					insert into solo_dining_favorite (id, user_id, place_id, created_at)
					values (1, 7, 12, timestamp '2026-07-03 13:20:00')
					""");
			statement.executeUpdate("""
					insert into solo_dining_favorite (id, user_id, place_id, created_at)
					values (2, 7, 12, timestamp '2026-07-03 13:21:00')
					""");
		}
	}

	private static void insertUtcBusinessTimestamps(final Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("""
					insert into user_account (id, role, status, phone_verified_at, onboarding_status, created_at)
					values (1, 'USER', 'ACTIVE', timestamp '2026-07-17 00:00:00', 'PHONE_VERIFIED', timestamp '2026-07-17 00:00:00')
					""");
			statement.executeUpdate("""
					insert into place_cache (id, google_place_id, name, latitude, longitude, business_status)
					values (1, 'place-1', '니어바이 카페', 37.56612000, 126.97845000, 'OPERATIONAL')
					""");
			statement.executeUpdate("""
					insert into solo_dining_favorite (id, user_id, place_id, created_at)
					values (1, 1, 1, timestamp '2026-07-17 00:00:00')
					""");
			statement.executeUpdate("""
					insert into phone_verification (id, user_id, phone_number, status, expires_at, verified_at)
					values (1, 1, '01012345678', 'VERIFIED', timestamp '2026-07-17 00:03:00', timestamp '2026-07-17 00:01:00')
					""");
			statement.executeUpdate("""
					insert into companion_post (
					    id, host_user_id, place_id, meeting_at, meeting_time_type, max_participants, content,
					    open_chat_url, status, created_at, exposure_expires_at, depart_even_if_not_full
					) values (
					    1, 1, 1, null, 'NOW', 2, '지금 같이 식사해요.',
					    'https://open.kakao.com/o/now', 'RECRUITING', timestamp '2026-07-17 00:00:00',
					    timestamp '2026-07-17 01:00:00', true
					), (
					    2, 1, 1, timestamp '2026-07-17 19:00:00', 'SCHEDULED', 2, '저녁 같이 식사해요.',
					    'https://open.kakao.com/o/scheduled', 'RECRUITING', timestamp '2026-07-17 00:00:00',
					    timestamp '2026-07-17 01:00:00', true
					)
					""");
			statement.executeUpdate("""
					insert into companion_application (id, post_id, applicant_user_id, status, created_at)
					values (1, 1, 2, 'ACCEPTED', timestamp '2026-07-17 00:00:00')
					""");
			statement.executeUpdate("""
					insert into companion_match (id, post_id, status, created_at)
					values (1, 1, 'MATCHED', timestamp '2026-07-17 00:00:00'),
					       (2, 2, 'MATCHED', timestamp '2026-07-17 00:00:00')
					""");
			statement.executeUpdate("""
					insert into companion_schedule (id, match_id, place_id, scheduled_at, confirmed)
					values (1, 1, 1, timestamp '2026-07-17 00:00:00', true),
					       (2, 2, 1, timestamp '2026-07-17 19:00:00', true)
					""");
			statement.executeUpdate("""
					insert into companion_meeting (id, match_id, status, started_at, completed_at)
					values (1, 1, 'COMPLETED', timestamp '2026-07-17 00:00:00', timestamp '2026-07-17 01:00:00'),
					       (2, 2, 'COMPLETED', timestamp '2026-07-17 19:00:00', timestamp '2026-07-17 20:00:00')
					""");
			statement.executeUpdate("""
					insert into meeting_check_in (id, meeting_id, user_id, latitude, longitude, checked_in_at, completed_at)
					values (1, 1, 1, 37.56612000, 126.97845000, timestamp '2026-07-17 00:00:00', timestamp '2026-07-17 01:00:00')
					""");
			statement.executeUpdate("""
					insert into companion_notification (
					    id, recipient_user_id, notification_type, target_type, target_id, read_at, created_at
					) values (
					    1, 1, 'COMPANION_APPLICATION_CREATED', 'COMPANION_APPLICATION', 1,
					    timestamp '2026-07-17 01:00:00', timestamp '2026-07-17 00:00:00'
					)
					""");
			statement.executeUpdate("""
					insert into companion_review (id, meeting_id, reviewer_user_id, reviewee_user_id, rating, created_at)
					values (1, 1, 1, 2, 5, timestamp '2026-07-17 00:00:00')
					""");
		}
	}

	private static void assertTimestamp(
			final Connection connection,
			final String sql,
			final String expected
	) throws SQLException {
		try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getTimestamp(1).toLocalDateTime()).isEqualTo(LocalDateTime.parse(expected));
		}
	}

	private static void insertDuplicateCompanionApplications(final Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("""
					insert into companion_post (
					    id,
					    host_user_id,
					    place_id,
					    meeting_at,
					    max_participants,
					    content,
					    open_chat_url,
					    status,
					    created_at
					) values (
					    10,
					    100,
					    20,
					    timestamp '2026-07-15 14:30:00',
					    4,
					    '같이 밥 먹을 동행을 구해요.',
					    'https://open.kakao.com/o/nearby123',
					    'RECRUITING',
					    timestamp '2026-07-15 11:30:00'
					)
					""");
				statement.executeUpdate("""
						insert into companion_application (
						    id,
					    post_id,
					    applicant_user_id,
					    status,
					    rejection_reason,
					    created_at
					) values (
					    1,
					    10,
					    7,
					    'PENDING',
					    null,
					    timestamp '2026-07-15 12:00:00'
					)
					""");
			statement.executeUpdate("""
					insert into companion_application (
					    id,
					    post_id,
					    applicant_user_id,
					    status,
					    rejection_reason,
					    created_at
					) values (
					    2,
					    10,
					    7,
					    'ACCEPTED',
					    null,
					    timestamp '2026-07-15 12:30:00'
						)
						""");
				statement.executeUpdate("""
						insert into companion_match (
						    id,
						    post_id,
						    status,
						    created_at
						) values (
						    30,
						    10,
						    'MATCHED',
						    timestamp '2026-07-15 12:40:00'
						)
						""");
				statement.executeUpdate("""
						insert into companion_match_participant (
						    id,
						    match_id,
						    user_id,
						    accepted_application_id,
						    role
						) values (
						    40,
						    30,
						    7,
						    1,
						    'GUEST'
						)
						""");
				statement.executeUpdate("""
						insert into companion_application_read_status (
						    id,
						    application_id,
						    user_id,
						    read_at
						) values (
						    50,
						    1,
						    100,
						    timestamp '2026-07-15 12:10:00'
						)
						""");
				statement.executeUpdate("""
						insert into companion_application_read_status (
						    id,
						    application_id,
						    user_id,
						    read_at
						) values (
						    51,
						    2,
						    100,
						    timestamp '2026-07-15 12:20:00'
						)
						""");
				statement.executeUpdate("""
						insert into companion_application_read_status (
						    id,
						    application_id,
						    user_id,
						    read_at
						) values (
						    52,
						    1,
						    101,
						    timestamp '2026-07-15 12:15:00'
						)
						""");
				statement.executeUpdate("""
						insert into companion_notification (
						    id,
						    recipient_user_id,
						    notification_type,
						    target_type,
						    target_id,
						    read_at,
						    created_at
						) values (
						    60,
						    100,
						    'COMPANION_APPLICATION_CREATED',
						    'COMPANION_APPLICATION',
						    1,
						    null,
						    timestamp '2026-07-15 12:10:00'
						)
						""");
				statement.executeUpdate("""
						insert into companion_notification (
						    id,
						    recipient_user_id,
						    notification_type,
						    target_type,
						    target_id,
						    read_at,
						    created_at
						) values (
						    61,
						    100,
						    'COMPANION_APPLICATION_CREATED',
						    'COMPANION_APPLICATION',
						    2,
						    null,
						    timestamp '2026-07-15 12:20:00'
						)
						""");
				statement.executeUpdate("""
						insert into companion_notification (
						    id,
						    recipient_user_id,
						    notification_type,
						    target_type,
						    target_id,
						    read_at,
						    created_at
						) values (
						    62,
						    101,
						    'COMPANION_APPLICATION_CREATED',
						    'COMPANION_APPLICATION',
						    1,
						    null,
						    timestamp '2026-07-15 12:15:00'
						)
						""");
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

	private static long soloDiningFavoriteCount(final Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("select count(*) from solo_dining_favorite")) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}

	private static long companionApplicationCount(final Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("select count(*) from companion_application")) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}

	private static long remainingCompanionApplicationId(final Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("select id from companion_application")) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}

	private static long remainingMatchParticipantApplicationId(final Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(
						"select accepted_application_id from companion_match_participant"
				)) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}

	private static List<Long> companionApplicationReadStatusApplicationIds(final Connection connection)
			throws SQLException {
		List<Long> applicationIds = new ArrayList<>();
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("""
						select application_id
						from companion_application_read_status
						order by id
						""")) {
			while (resultSet.next()) {
				applicationIds.add(resultSet.getLong(1));
			}
		}
		return applicationIds;
	}

	private static List<Long> companionNotificationTargetIds(final Connection connection) throws SQLException {
		List<Long> targetIds = new ArrayList<>();
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("""
						select target_id
						from companion_notification
						order by id
						""")) {
			while (resultSet.next()) {
				targetIds.add(resultSet.getLong(1));
			}
		}
		return targetIds;
	}

	private static long remainingSoloDiningFavoriteId(final Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("select id from solo_dining_favorite")) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}
}
