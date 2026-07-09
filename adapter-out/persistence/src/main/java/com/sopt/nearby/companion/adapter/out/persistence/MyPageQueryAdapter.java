// 마이페이지 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.MyPageProfileProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MyPageQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MyPageVisitedPlaceProjection;
import com.sopt.nearby.companion.domain.model.profile.MyPageProfile;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.out.MyPageQueryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyPageQueryAdapter implements MyPageQueryPort {

    private final MyPageQueryJpaRepository repository;

    public MyPageQueryAdapter(final MyPageQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MyPageProfile> findByUserId(final Long userId) {
        return repository.findProfileByUserId(userId)
                .map(row -> toProfile(
                        row,
                        repository.findTravelStyleKeywordsByProfileId(row.getProfileId()),
                        mannerKeywords(userId),
                        completedMeetingPlaces(userId)
                ));
    }

    private List<ReviewKeyword> mannerKeywords(final Long userId) {
        return repository.findReceivedReviewKeywordsByUserId(userId)
                .stream()
                .map(ReviewKeyword::valueOf)
                .toList();
    }

    private List<MyPageProfile.CompletedMeetingPlace> completedMeetingPlaces(final Long userId) {
        return repository.findCompletedMeetingPlacesByUserId(userId)
                .stream()
                .map(this::toCompletedMeetingPlace)
                .toList();
    }

    private MyPageProfile toProfile(
            final MyPageProfileProjection row,
            final List<TravelStyleKeyword> travelStyleKeywords,
            final List<ReviewKeyword> mannerKeywords,
            final List<MyPageProfile.CompletedMeetingPlace> completedMeetingPlaces
    ) {
        return new MyPageProfile(
                row.getProfileId(),
                row.getUserId(),
                row.getNickname(),
                UserGender.valueOf(row.getGender()),
                row.getBirthYear(),
                row.getProfileImageUrl(),
                row.getMannerScore(),
                row.getReviewCount().intValue(),
                row.getPhoneVerifiedAt(),
                travelStyleKeywords,
                mannerKeywords,
                completedMeetingPlaces
        );
    }

    private MyPageProfile.CompletedMeetingPlace toCompletedMeetingPlace(final MyPageVisitedPlaceProjection row) {
        return new MyPageProfile.CompletedMeetingPlace(
                row.getPlaceName(),
                row.getPlaceAddress()
        );
    }
}
