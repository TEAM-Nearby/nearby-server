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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class MyPageQueryAdapter implements MyPageQueryPort {

    private static final Logger log = LoggerFactory.getLogger(MyPageQueryAdapter.class);

    private final MyPageQueryJpaRepository repository;

    public MyPageQueryAdapter(final MyPageQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MyPageProfile> findByUserId(final Long userId) {
        return repository.findProfileByUserId(userId)
                .flatMap(row -> toProfile(row, userId));
    }

    private List<ReviewKeyword> mannerKeywords(final Long userId) {
        return repository.findReceivedReviewKeywordsByUserId(userId)
                .stream()
                .map(keyword -> parseReviewKeyword(userId, keyword))
                .flatMap(Optional::stream)
                .toList();
    }

    private List<MyPageProfile.CompletedMeetingPlace> completedMeetingPlaces(final Long userId) {
        return repository.findCompletedMeetingPlacesByUserId(userId)
                .stream()
                .map(this::toCompletedMeetingPlace)
                .toList();
    }

    private Optional<MyPageProfile> toProfile(
            final MyPageProfileProjection row,
            final Long userId
    ) {
        Optional<UserGender> gender = parseGender(row);
        if (gender.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new MyPageProfile(
                row.getProfileId(),
                row.getUserId(),
                row.getNickname(),
                gender.get(),
                row.getBirthYear(),
                row.getProfileImageUrl(),
                row.getMannerScore(),
                reviewCount(row),
                row.getPhoneVerifiedAt(),
                repository.findTravelStyleKeywordsByProfileId(row.getProfileId()),
                mannerKeywords(userId),
                completedMeetingPlaces(userId)
        ));
    }

    private int reviewCount(final MyPageProfileProjection row) {
        if (row.getReviewCount() == null) {
            log.warn(
                    "Using default review count for my page profile. profileId={}, userId={}",
                    row.getProfileId(),
                    row.getUserId()
            );
            return 0;
        }
        return row.getReviewCount();
    }

    private Optional<UserGender> parseGender(final MyPageProfileProjection row) {
        try {
            return Optional.of(UserGender.valueOf(row.getGender()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            log.warn(
                    "Skipping my page profile with invalid gender. profileId={}, userId={}, gender={}",
                    row.getProfileId(),
                    row.getUserId(),
                    row.getGender()
            );
            return Optional.empty();
        }
    }

    private Optional<ReviewKeyword> parseReviewKeyword(final Long userId, final String keyword) {
        try {
            return Optional.of(ReviewKeyword.valueOf(keyword));
        } catch (IllegalArgumentException | NullPointerException exception) {
            log.warn("Skipping invalid my page review keyword. userId={}, keyword={}", userId, keyword);
            return Optional.empty();
        }
    }

    private MyPageProfile.CompletedMeetingPlace toCompletedMeetingPlace(final MyPageVisitedPlaceProjection row) {
        return new MyPageProfile.CompletedMeetingPlace(
                row.getPlaceName(),
                row.getPlaceAddress()
        );
    }
}
