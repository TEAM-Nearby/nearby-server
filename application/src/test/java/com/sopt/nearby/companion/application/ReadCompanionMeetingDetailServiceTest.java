// 진행 중인 동행 상세 조회 서비스의 권한과 상태 검증을 확인하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenReadCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.exception.ReadCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.ReadCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingDetail;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionMeetingDetailQueryPort;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionMeetingDetailServiceTest {

    private static final Long MEETING_ID = 1L;
    private static final Long USER_ID = 7L;

    private FakeCompanionMeetingDetailQueryPort queryPort;
    private ReadCompanionMeetingDetailService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeCompanionMeetingDetailQueryPort();
        service = new ReadCompanionMeetingDetailService(queryPort);
        queryPort.put(detail(MatchParticipantRole.GUEST, CompanionMeetingStatus.ONGOING));
    }

    @Test
    void returnsOngoingMeetingDetailWithHostGender() {
        ReadCompanionMeetingDetailResult result = service.getDetail(MEETING_ID, USER_ID);

        assertEquals(MEETING_ID, result.meetingId());
        assertEquals(MatchParticipantRole.GUEST, result.currentUserRole());
        assertEquals(1L, result.hostId());
        assertEquals(UserGender.FEMALE, result.hostGender());
        assertEquals("https://image.url/profile.png", result.hostProfileImageUrl());
        assertEquals("정지영", result.hostNickname());
        assertEquals(true, result.hostCheckedIn());
        assertEquals("시우다드 콘달", result.placeName());
        assertEquals(LocalDateTime.of(2026, 6, 29, 18, 30), result.meetingAt());
        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, result.meetingTimeType());
        assertEquals(CompanionMeetingStatus.ONGOING, result.meetingStatus());
        assertEquals(false, result.currentUserCheckedIn());
        assertEquals(true, result.canCancelMeeting());
    }

    @Test
    void returnsHostRoleWhenRequesterIsHost() {
        queryPort.put(detail(MatchParticipantRole.HOST, CompanionMeetingStatus.ONGOING));

        ReadCompanionMeetingDetailResult result = service.getDetail(MEETING_ID, USER_ID);

        assertEquals(MatchParticipantRole.HOST, result.currentUserRole());
        assertEquals(1L, result.hostId());
        assertEquals(UserGender.FEMALE, result.hostGender());
    }

    @Test
    void rejectsInvalidMeetingId() {
        assertThrows(InvalidCompanionMeetingIdException.class, () -> service.getDetail(null, USER_ID));
        assertThrows(InvalidCompanionMeetingIdException.class, () -> service.getDetail(0L, USER_ID));
    }

    @Test
    void rejectsMissingMeetingAndForbiddenUser() {
        queryPort.details.clear();

        assertThrows(CompanionMeetingNotFoundException.class, () -> service.getDetail(MEETING_ID, USER_ID));

        queryPort.put(detail(null, CompanionMeetingStatus.ONGOING));

        assertThrows(ForbiddenReadCompanionMeetingException.class, () -> service.getDetail(MEETING_ID, USER_ID));
    }

    @Test
    void rejectsCanceledOrCompletedMeeting() {
        queryPort.put(detail(MatchParticipantRole.GUEST, CompanionMeetingStatus.CANCELED));

        assertThrows(ReadCompanionMeetingAlreadyCanceledException.class, () -> service.getDetail(MEETING_ID, USER_ID));

        queryPort.put(detail(MatchParticipantRole.GUEST, CompanionMeetingStatus.COMPLETED));

        assertThrows(ReadCompanionMeetingAlreadyCompletedException.class, () -> service.getDetail(MEETING_ID, USER_ID));
    }

    private CompanionMeetingDetail detail(
            final MatchParticipantRole currentUserRole,
            final CompanionMeetingStatus meetingStatus
    ) {
        return new CompanionMeetingDetail(
                MEETING_ID,
                currentUserRole,
                1L,
                UserGender.FEMALE,
                "https://image.url/profile.png",
                "정지영",
                true,
                "시우다드 콘달",
                LocalDateTime.of(2026, 6, 29, 18, 30),
                CompanionPostMeetingTimeType.SCHEDULED,
                meetingStatus,
                false
        );
    }

    private static final class FakeCompanionMeetingDetailQueryPort implements CompanionMeetingDetailQueryPort {

        private final Map<Long, CompanionMeetingDetail> details = new HashMap<>();

        @Override
        public Optional<CompanionMeetingDetail> findByMeetingIdAndUserId(final Long meetingId, final Long userId) {
            return Optional.ofNullable(details.get(meetingId));
        }

        private void put(final CompanionMeetingDetail detail) {
            details.put(detail.meetingId(), detail);
        }
    }
}
