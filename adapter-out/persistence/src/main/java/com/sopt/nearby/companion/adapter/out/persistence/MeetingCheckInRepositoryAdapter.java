// 미팅 체크인 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MeetingCheckInRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<MeetingCheckIn, Long, MeetingCheckInEntity, Long>
		implements MeetingCheckInRepository {

	private final MeetingCheckInJpaRepository jpaRepository;
	private final EntityManager entityManager;

	public MeetingCheckInRepositoryAdapter(
			final MeetingCheckInJpaRepository jpaRepository,
			final EntityManager entityManager
	) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
		this.entityManager = entityManager;
	}

	@Override
	public Optional<MeetingCheckIn> findByMeetingIdAndUserId(final Long meetingId, final Long userId) {
		return jpaRepository.findByMeetingIdAndUserId(meetingId, userId)
				.map(CompanionPersistenceMapper::toDomain);
	}

	@Override
	public long countByMeetingId(final Long meetingId) {
		return jpaRepository.countByMeetingId(meetingId);
	}

	@Override
	@Transactional
	public MeetingCheckIn saveIfAbsent(final MeetingCheckIn checkIn) {
		Optional<MeetingCheckIn> existingCheckIn = findByMeetingIdAndUserId(checkIn.meetingId(), checkIn.userId());
		if (existingCheckIn.isPresent()) {
			return existingCheckIn.get();
		}
		insertWithSavepoint(checkIn);
		return findByMeetingIdAndUserId(checkIn.meetingId(), checkIn.userId())
				.orElseThrow(() -> new IllegalStateException("만남 인증 저장 후 체크인 정보를 찾을 수 없습니다."));
	}

	private void insertWithSavepoint(final MeetingCheckIn checkIn) {
		entityManager.unwrap(Session.class).doWork(connection -> {
			Savepoint savepoint = connection.setSavepoint("meeting_check_in_insert");
			try (PreparedStatement statement = connection.prepareStatement("""
					insert into meeting_check_in (meeting_id, user_id, latitude, longitude, checked_in_at)
					values (?, ?, ?, ?, ?)
					""")) {
				statement.setLong(1, checkIn.meetingId());
				statement.setLong(2, checkIn.userId());
				statement.setBigDecimal(3, checkIn.latitude());
				statement.setBigDecimal(4, checkIn.longitude());
				statement.setObject(5, checkIn.checkedInAt());
				statement.executeUpdate();
				connection.releaseSavepoint(savepoint);
			} catch (SQLException exception) {
				connection.rollback(savepoint);
				if (!isUniqueConstraintViolation(exception)) {
					throw exception;
				}
			}
		});
	}

	private boolean isUniqueConstraintViolation(final SQLException exception) {
		return "23505".equals(exception.getSQLState());
	}
}
