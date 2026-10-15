// 동행 프로필 상세 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileDetailProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileDetailQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileDetail;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.out.CompanionProfileDetailQueryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionProfileDetailQueryAdapter implements CompanionProfileDetailQueryPort {

    private final CompanionProfileDetailQueryJpaRepository repository;

    public CompanionProfileDetailQueryAdapter(final CompanionProfileDetailQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CompanionProfileDetail> findByProfileId(final Long profileId) {
        return repository.findDetailByProfileId(profileId)
                .map(row -> toDetail(
                        row,
                        repository.findKeywordsByProfileId(row.getProfileId()),
                        mannerKeywords(row.getUserId())
                ));
    }

    private CompanionProfileDetail toDetail(
            final CompanionProfileDetailProjection row,
            final List<TravelStyleKeyword> keywords,
            final List<ReviewKeyword> mannerKeywords
    ) {
        return new CompanionProfileDetail(
                row.getProfileId(),
                row.getUserId(),
                row.getNickname(),
                UserGender.valueOf(row.getGender()),
                row.getBirthYear(),
                row.getProfileImageUrl(),
                row.getIntro(),
                row.getMannerScore(),
                row.getReviewCount(),
                CompanionProfileStatus.valueOf(row.getStatus()),
                row.getPhoneVerifiedAt(),
                keywords,
                mannerKeywords
        );
    }

    private List<ReviewKeyword> mannerKeywords(final Long userId) {
        return repository.findMannerKeywordsByUserId(userId)
                .stream()
                .map(this::toReviewKeyword)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ReviewKeyword> toReviewKeyword(final String value) {
        try {
            return Optional.of(ReviewKeyword.valueOf(value));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Optional.empty();
        }
    }
}
