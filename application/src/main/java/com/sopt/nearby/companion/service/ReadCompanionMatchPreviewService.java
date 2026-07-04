//매칭된 동행 미리보기 서비스 로직
package com.sopt.nearby.companion.service;


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
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;

import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import java.util.List;


public class ReadCompanionMatchPreviewService implements ReadCompanionMatchPreviewUseCase {
    private final CompanionMatchRepository companionMatchRepository;
    private final CompanionPostRepository companionPostRepository;
    private final CompanionMatchParticipantRepository companionMatchParticipantRepository;
    private final CompanionProfileRepository companionProfileRepository;

    public ReadCompanionMatchPreviewService(CompanionMatchRepository companionMatchRepository,
                                            CompanionPostRepository companionPostRepository,
                                            CompanionMatchParticipantRepository companionMatchParticipantRepository,
                                            CompanionProfileRepository companionProfileRepository) {
        this.companionMatchRepository = companionMatchRepository;
        this.companionPostRepository = companionPostRepository;
        this.companionMatchParticipantRepository = companionMatchParticipantRepository;
        this.companionProfileRepository = companionProfileRepository;
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
                CompanionPostNotFoundException::new);
        List<CompanionMatchParticipant> participants = companionMatchParticipantRepository.findAllByMatchId(match.id());
        List<CompanionProfile> profiles = companionProfileRepository.findAllByUserIdIn(participants.stream().map(
                CompanionMatchParticipant::userId
        ).toList());

        List<CompanionMatchPreview.Member> members = participants.stream().map(
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

        Post previewPost = new Post(
                post.id(), post.content()
        );

        return new CompanionMatchPreview(match.id(), members, previewPost);
    }
}
