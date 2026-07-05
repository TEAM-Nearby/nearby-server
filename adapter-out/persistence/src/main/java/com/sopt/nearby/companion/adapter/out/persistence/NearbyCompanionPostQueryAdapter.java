// 주변 동행 모집글 목록 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.NearbyCompanionPostProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.NearbyCompanionPostQueryJpaRepository;
import com.sopt.nearby.companion.application.ReadNearbyCompanionPostsCommand;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.post.NearbyCompanionPostSummary;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.NearbyCompanionPostQueryPort;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class NearbyCompanionPostQueryAdapter implements NearbyCompanionPostQueryPort {

    private final NearbyCompanionPostQueryJpaRepository repository;

    public NearbyCompanionPostQueryAdapter(final NearbyCompanionPostQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<NearbyCompanionPostSummary> findNearby(final ReadNearbyCompanionPostsCommand command) {
        return repository.findNearby(
                        command.latitude(),
                        command.longitude(),
                        command.radiusMeters(),
                        command.placeCategory().name(),
                        command.sort().name()
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private NearbyCompanionPostSummary toSummary(final NearbyCompanionPostProjection row) {
        return new NearbyCompanionPostSummary(
                row.getPostId(),
                CompanionPostStatus.valueOf(row.getStatus()),
                row.getHostNickname(),
                UserGender.valueOf(row.getHostGender()),
                row.getPlaceId(),
                row.getGooglePlaceId(),
                row.getPlaceName(),
                toPlaceCategory(row.getPlaceCategory()),
                row.getLatitude(),
                row.getLongitude(),
                row.getDistanceMeters().intValue(),
                row.getPhotoReference(),
                row.getContent(),
                row.getMeetingAt(),
                row.getParticipantCount().intValue(),
                row.getMaxParticipants().intValue(),
                row.getCreatedAt()
        );
    }

    private CompanionPostPlaceCategory toPlaceCategory(final String value) {
        try {
            return CompanionPostPlaceCategory.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return CompanionPostPlaceCategory.OTHER;
        }
    }
}
