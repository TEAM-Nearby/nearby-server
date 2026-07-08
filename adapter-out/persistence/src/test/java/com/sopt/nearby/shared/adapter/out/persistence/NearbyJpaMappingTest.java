// ERD 기반 JPA 엔티티와 Spring Data Repository 구성을 검증하는 테스트
package com.sopt.nearby.shared.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostStyleEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportReasonEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReportJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningFavoriteJpaRepository;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class NearbyJpaMappingTest {

	@Autowired
	private org.springframework.context.ApplicationContext applicationContext;

	@Autowired
	private UserAccountJpaRepository userAccountJpaRepository;

	@Test
	void loadsJpaMappingsAndRepositories() {
		assertThat(applicationContext.getBean(UserAccountJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(PlaceCacheJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(SoloDiningFavoriteJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionProfileJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionPostJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionApplicationJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionMatchJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionMeetingJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionReportJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionReviewJpaRepository.class)).isNotNull();
	}

	@Test
	void persistsUserAccountWithEnumStringColumns() {
		UserAccountEntity entity = new UserAccountEntity(
				null,
				UserRole.USER,
				UserAccountStatus.ACTIVE,
				"01012345678",
				null,
				UserOnboardingStatus.STARTED,
				LocalDateTime.now(),
				null
		);

		UserAccountEntity saved = userAccountJpaRepository.saveAndFlush(entity);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getRole()).isEqualTo(UserRole.USER);
		assertThat(saved.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
		assertThat(saved.getOnboardingStatus()).isEqualTo(UserOnboardingStatus.STARTED);
	}

	@Test
	void coordinateColumnsUseGoogleMapsPrecisionAndScale() throws NoSuchFieldException {
		assertCoordinateColumn(MeetingCheckInEntity.class, "latitude");
		assertCoordinateColumn(MeetingCheckInEntity.class, "longitude");
		assertCoordinateColumn(PlaceCacheEntity.class, "latitude");
		assertCoordinateColumn(PlaceCacheEntity.class, "longitude");
	}

	@Test
	void idClassesExposePublicNoArgConstructors() throws NoSuchMethodException {
		assertPublicNoArgConstructor(CompanionProfileStyleEntityId.class);
		assertPublicNoArgConstructor(CompanionPostStyleEntityId.class);
		assertPublicNoArgConstructor(CompanionReportReasonEntityId.class);
		assertPublicNoArgConstructor(CompanionReviewKeywordEntityId.class);
	}

	@Test
	void companionReportPreventsDuplicateReportsForSameMeetingAndUsers() {
		Table table = CompanionReportEntity.class.getAnnotation(Table.class);

		assertThat(table).isNotNull();
		assertThat(table.uniqueConstraints())
				.singleElement()
				.satisfies(NearbyJpaMappingTest::assertCompanionReportUniqueConstraint);
	}

	@Test
	void soloDiningFavoritePreventsDuplicateFavoriteForSamePlaceAndUser() {
		Table table = SoloDiningFavoriteEntity.class.getAnnotation(Table.class);

		assertThat(table).isNotNull();
		assertThat(table.uniqueConstraints())
				.singleElement()
				.satisfies(NearbyJpaMappingTest::assertSoloDiningFavoriteUniqueConstraint);
	}

	private void assertCoordinateColumn(final Class<?> entityType, final String fieldName) throws NoSuchFieldException {
		Column column = entityType.getDeclaredField(fieldName).getAnnotation(Column.class);

		assertThat(column).isNotNull();
		assertThat(column.precision()).isEqualTo(11);
		assertThat(column.scale()).isEqualTo(8);
	}

	private static void assertCompanionReportUniqueConstraint(final UniqueConstraint uniqueConstraint) {
		assertThat(uniqueConstraint.name()).isEqualTo("uk_companion_report_meeting_reporter_reported");
		assertThat(uniqueConstraint.columnNames())
				.containsExactly("meeting_id", "reporter_user_id", "reported_user_id");
	}

	private static void assertSoloDiningFavoriteUniqueConstraint(final UniqueConstraint uniqueConstraint) {
		assertThat(uniqueConstraint.name()).isEqualTo("uk_solo_dining_favorite_user_place");
		assertThat(uniqueConstraint.columnNames())
				.containsExactly("user_id", "place_id");
	}

	private void assertPublicNoArgConstructor(final Class<?> idClass) throws NoSuchMethodException {
		assertThat(Modifier.isPublic(idClass.getDeclaredConstructor().getModifiers())).isTrue();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			UserAccountEntity.class,
			PlaceCacheEntity.class,
			CompanionProfileEntity.class,
			CompanionPostEntity.class,
			CompanionApplicationEntity.class,
			CompanionMatchEntity.class,
			CompanionMeetingEntity.class,
			CompanionReportEntity.class,
			CompanionReviewEntity.class
	})
	@EnableJpaRepositories(basePackages = "com.sopt.nearby")
	static class TestApplication {
	}
}
