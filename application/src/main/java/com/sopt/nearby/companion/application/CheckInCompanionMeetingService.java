// 동행 만남 인증 검증과 저장을 처리하는 유스케이스 구현체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CheckInTimeNotAllowedException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionScheduleNotConfirmedException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCheckInRequestException;
import com.sopt.nearby.companion.domain.exception.OutOfCheckInRadiusException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingCheckInContext;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.port.in.CheckInCompanionMeetingUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingCheckInQueryPort;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class CheckInCompanionMeetingService implements CheckInCompanionMeetingUseCase {

    private static final double ALLOWED_RADIUS_METERS = 150.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final CompanionMeetingCheckInQueryPort queryPort;
    private final CompanionMatchParticipantRepository participantRepository;
    private final MeetingCheckInRepository checkInRepository;
    private final Clock clock;

    public CheckInCompanionMeetingService(
            final CompanionMeetingCheckInQueryPort queryPort,
            final CompanionMatchParticipantRepository participantRepository,
            final MeetingCheckInRepository checkInRepository,
            final Clock clock
    ) {
        this.queryPort = queryPort;
        this.participantRepository = participantRepository;
        this.checkInRepository = checkInRepository;
        this.clock = clock;
    }

    @Override
    public CheckInCompanionMeetingResult checkIn(final CheckInCompanionMeetingCommand command) {
        validateCommand(command);

        CompanionMeetingCheckInContext context = queryPort.findByMeetingId(command.meetingId())
                .orElseThrow(CompanionMeetingNotFoundException::new);
        List<CompanionMatchParticipant> participants = participantRepository.findAllByMatchId(context.matchId());
        if (participants.stream().noneMatch(participant -> participant.userId().equals(command.userId()))) {
            throw new ForbiddenCompanionMeetingException();
        }

        validateMeetingStatus(context.meetingStatus());
        if (!context.hasConfirmedSchedulePlace()) {
            throw new CompanionScheduleNotConfirmedException();
        }

        LocalDateTime availableFrom = context.scheduledAt().minusHours(1);
        LocalDateTime availableUntil = context.scheduledAt().plusHours(1);
        double distanceMeters = calculateDistanceMeters(context, command);

        return checkInRepository.findByMeetingIdAndUserId(command.meetingId(), command.userId())
                .map(existingCheckIn -> toResult(
                        context,
                        existingCheckIn,
                        participants.size(),
                        distanceMeters,
                        availableFrom,
                        availableUntil,
                        true
                ))
                .orElseGet(() -> checkInNewParticipant(
                        command,
                        context,
                        participants.size(),
                        distanceMeters,
                        availableFrom,
                        availableUntil
                ));
    }

    private CheckInCompanionMeetingResult checkInNewParticipant(
            final CheckInCompanionMeetingCommand command,
            final CompanionMeetingCheckInContext context,
            final int totalParticipantCount,
            final double distanceMeters,
            final LocalDateTime availableFrom,
            final LocalDateTime availableUntil
    ) {
        validateCheckInTime(availableFrom, availableUntil);
        if (distanceMeters > ALLOWED_RADIUS_METERS) {
            throw new OutOfCheckInRadiusException();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        MeetingCheckIn savedCheckIn = checkInRepository.saveIfAbsent(new MeetingCheckIn(
                null,
                command.meetingId(),
                command.userId(),
                command.latitude(),
                command.longitude(),
                now,
                null
        ));
        boolean alreadyCompleted = !savedCheckIn.checkedInAt().equals(now);

        return toResult(
                context,
                savedCheckIn,
                totalParticipantCount,
                distanceMeters,
                availableFrom,
                availableUntil,
                alreadyCompleted
        );
    }

    private void validateCommand(final CheckInCompanionMeetingCommand command) {
        if (command == null
                || command.userId() == null
                || command.meetingId() == null
                || command.meetingId() <= 0
                || isOutOfRange(command.latitude(), MIN_LATITUDE, MAX_LATITUDE)
                || isOutOfRange(command.longitude(), MIN_LONGITUDE, MAX_LONGITUDE)) {
            throw new InvalidCheckInRequestException();
        }
    }

    private boolean isOutOfRange(final BigDecimal value, final BigDecimal min, final BigDecimal max) {
        return value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0;
    }

    private void validateMeetingStatus(final CompanionMeetingStatus status) {
        if (status == CompanionMeetingStatus.CANCELED) {
            throw new CompanionMeetingAlreadyCanceledException();
        }
        if (status == CompanionMeetingStatus.COMPLETED) {
            throw new CompanionMeetingAlreadyCompletedException();
        }
    }

    private void validateCheckInTime(final LocalDateTime availableFrom, final LocalDateTime availableUntil) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(availableFrom) || now.isAfter(availableUntil)) {
            throw new CheckInTimeNotAllowedException();
        }
    }

    private CheckInCompanionMeetingResult toResult(
            final CompanionMeetingCheckInContext context,
            final MeetingCheckIn checkIn,
            final long totalParticipantCount,
            final double distanceMeters,
            final LocalDateTime availableFrom,
            final LocalDateTime availableUntil,
            final boolean alreadyCompleted
    ) {
        long checkedInCount = checkInRepository.countByMeetingId(context.meetingId());
        boolean allParticipantsCheckedIn = checkedInCount >= totalParticipantCount;

        return new CheckInCompanionMeetingResult(
                context.meetingId(),
                context.meetingStatus(),
                true,
                checkedInCount,
                totalParticipantCount,
                allParticipantsCheckedIn,
                allParticipantsCheckedIn,
                checkIn.checkedInAt(),
                roundToOneDecimal(distanceMeters),
                ALLOWED_RADIUS_METERS,
                availableFrom,
                availableUntil,
                alreadyCompleted
        );
    }

    private double calculateDistanceMeters(
            final CompanionMeetingCheckInContext context,
            final CheckInCompanionMeetingCommand command
    ) {
        double placeLatitude = Math.toRadians(context.placeLatitude().doubleValue());
        double userLatitude = Math.toRadians(command.latitude().doubleValue());
        double deltaLatitude = Math.toRadians(command.latitude().subtract(context.placeLatitude()).doubleValue());
        double deltaLongitude = Math.toRadians(command.longitude().subtract(context.placeLongitude()).doubleValue());


        /*
            하버사인 공식(Haversine formula)
            => 사용자 현재 위치가 만남 장소에서 몇 m 떨어져 있는지 계산하려고 넣은 코드
         */
        double haversine = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(placeLatitude) * Math.cos(userLatitude)
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
        // haversine 값을 무조건 0에서 1 사이로 제한한다는 뜻입니다.
        haversine = Math.clamp(haversine, 0.0, 1.0);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return EARTH_RADIUS_METERS * centralAngle;
    }

    private double roundToOneDecimal(final double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
