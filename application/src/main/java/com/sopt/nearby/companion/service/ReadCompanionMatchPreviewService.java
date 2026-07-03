//실제 조회 흐름 담당
//매칭 조회, 참여자 조회, 게시글 조회, 권한 검증을 조립
package com.sopt.nearby.companion.service;


import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMatchException;
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

    public CompanionMatchPreview getPreview(Long matchId) {
        //Todo 로그인 구현 시 userId를 활용한 검증 로직 추가
//        if (userId == null) {
//            throw new ForbiddenCompanionMatchException();
//        }

        CompanionMatch match = companionMatchRepository.findById(matchId)
                .orElseThrow(CompanionMatchNotFoundException::new);

        CompanionPost post = companionPostRepository.findById(match.postId()).orElseThrow(
                CompanionMatchNotFoundException::new);
        List<CompanionMatchParticipant> participants = companionMatchParticipantRepository.findAllByMatchId(match.id());
        List<CompanionProfile> profiles = companionProfileRepository.findAllByUserIdIn(participants.stream().map(
                CompanionMatchParticipant::userId
        ).toList());

        List<CompanionMatchPreview.Member> members = participants.stream().map(
                participant -> {
                    CompanionProfile profile = profiles.stream()
                            .filter(p -> p.userId().equals(participant.userId()))
                            .findFirst()
                            .orElseThrow();
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
