// 미팅 체크인 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MeetingCheckInRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<MeetingCheckIn, Long, MeetingCheckInEntity, Long>
		implements MeetingCheckInRepository {

	private static final String POSTGRES_INSERT_IGNORE_SQL = """
			insert into meeting_check_in (meeting_id, user_id, latitude, longitude, checked_in_at)
			values (:meetingId, :userId, :latitude, :longitude, :checkedInAt)
			on conflict (meeting_id, user_id) do nothing
			""";

	private static final String H2_INSERT_IGNORE_SQL = """
			insert into meeting_check_in (meeting_id, user_id, latitude, longitude, checked_in_at)
			select :meetingId, :userId, :latitude, :longitude, :checkedInAt
			where not exists (
				select 1
				from meeting_check_in
				where meeting_id = :meetingId
					and user_id = :userId
			)
			""";

	private final MeetingCheckInJpaRepository jpaRepository;
	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final String insertIgnoreSql;

	public MeetingCheckInRepositoryAdapter(
			final MeetingCheckInJpaRepository jpaRepository,
			final DataSource dataSource
	) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.insertIgnoreSql = insertIgnoreSql(dataSource);
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
	public long countCompletedByMeetingId(final Long meetingId) {
		return jpaRepository.countByMeetingIdAndCompletedAtIsNotNull(meetingId);
	}

	@Override
	@Transactional
	public MeetingCheckIn saveIfAbsent(final MeetingCheckIn checkIn) {
		insertIgnore(checkIn);
		return findByMeetingIdAndUserId(checkIn.meetingId(), checkIn.userId())
				.orElseThrow(() -> new IllegalStateException("만남 인증 저장 후 체크인 정보를 찾을 수 없습니다."));
	}

	private void insertIgnore(final MeetingCheckIn checkIn) {
		jdbcTemplate.update(insertIgnoreSql, new MapSqlParameterSource()
				.addValue("meetingId", checkIn.meetingId())
				.addValue("userId", checkIn.userId())
				.addValue("latitude", checkIn.latitude())
				.addValue("longitude", checkIn.longitude())
				.addValue("checkedInAt", checkIn.checkedInAt()));
	}

	private String insertIgnoreSql(final DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			if ("H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
				return H2_INSERT_IGNORE_SQL;
			}
			return POSTGRES_INSERT_IGNORE_SQL;
		} catch (SQLException exception) {
			throw new IllegalStateException("만남 인증 저장 SQL을 선택할 수 없습니다.", exception);
		}
	}
}
