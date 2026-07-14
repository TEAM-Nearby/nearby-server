//매칭된 동행 미리보기 서비스 로직
package com.sopt.nearby.companion.application;


import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMatchException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMatchIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview.Member;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview.Post;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;

import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public class ReadCompanionMatchPreviewService implements ReadCompanionMatchPreviewUseCase {
    private final CompanionMatchRepository companionMatchRepository;
    private final CompanionPostRepository companionPostRepository;
    private final CompanionMatchParticipantRepository companionMatchParticipantRepository;
    private final CompanionProfileRepository companionProfileRepository;
    private final CompanionScheduleRepository companionScheduleRepository;
    private final CompanionMatchSummaryQueryPort companionMatchSummaryQueryPort;

    public ReadCompanionMatchPreviewService(CompanionMatchRepository companionMatchRepository,
                                            CompanionPostRepository companionPostRepository,
                                            CompanionMatchParticipantRepository companionMatchParticipantRepository,
                                            CompanionProfileRepository companionProfileRepository,
                                            CompanionScheduleRepository companionScheduleRepository,
                                            CompanionMatchSummaryQueryPort companionMatchSummaryQueryPort) {
        this.companionMatchRepository = companionMatchRepository;
        this.companionPostRepository = companionPostRepository;
        this.companionMatchParticipantRepository = companionMatchParticipantRepository;
        this.companionProfileRepository = companionProfileRepository;
        this.companionScheduleRepository = companionScheduleRepository;
        this.companionMatchSummaryQueryPort = companionMatchSummaryQueryPort;
    }

    @Override
    public CompanionMatchPreview getPreview(final Long matchId, final Long userId) {
        if (matchId == null || matchId <= 0) {
            throw new InvalidCompanionMatchIdException();
        }

        CompanionMatch match = companionMatchRepository.findById(matchId)
                .orElseThrow(CompanionMatchNotFoundException::new);

        boolean joined = companionMatchParticipantRepository.existsByMatchIdAndUserId(match.id(), userId);

        if (!joined) {
            throw new ForbiddenCompanionMatchException();
        }

        CompanionPost post = companionPostRepository.findById(match.postId()).orElseThrow(
                CompanionPostNotFoundException::matchPostNotFound);
        List<CompanionMatchParticipant> participants = companionMatchParticipantRepository.findAllByMatchId(match.id());
        List<CompanionProfile> profiles = companionProfileRepository.findAllByUserIdIn(participants.stream().map(
                CompanionMatchParticipant::userId
        ).toList());

        List<CompanionMatchPreview.Member> participantsWithProfiles = participants.stream().map(
                participant -> {
                    CompanionProfile profile = profiles.stream()
                            .filter(p -> p.userId().equals(participant.userId()))
                            .findFirst()
                            .orElseThrow(CompanionProfileNotFoundException::new);
                    return new Member(
                            participant.userId(),
                            profile.profileImageUrl(),
                            profile.nickname()
                    );
                }
        ).toList();
        Member host = participantsWithProfiles.stream()
                .filter(member -> member.userId().equals(post.hostUserId()))
                .findFirst()
                .orElseThrow(CompanionProfileNotFoundException::new);
        List<Member> members = participantsWithProfiles.stream()
                .filter(member -> !member.userId().equals(post.hostUserId()))
                .toList();

        Optional<CompanionSchedule> confirmedSchedule = companionScheduleRepository.findConfirmedByMatchId(match.id());
        LocalDateTime meetingAt = confirmedSchedule
                .map(CompanionSchedule::scheduledAt)
                .orElseGet(() -> resolveMeetingAt(post));
        Long placeId = confirmedSchedule
                .map(CompanionSchedule::placeId)
                .orElse(post.placeId());

        Post previewPost = new Post(
                post.id(),
                post.content(),
                companionMatchSummaryQueryPort.findPlaceNameByPlaceId(placeId)
                        .orElseThrow(CompanionPostNotFoundException::matchPostNotFound),
                post.meetingTimeType(),
                meetingAt
        );

        return new CompanionMatchPreview(match.id(), host, members, previewPost);
    }

    private LocalDateTime resolveMeetingAt(final CompanionPost post) {
        return switch (post.meetingTimeType()) {
            case NOW -> post.exposureExpiresAt();
            case SCHEDULED -> post.meetingAt();
            case UNDECIDED -> null;
        };
    }
}
