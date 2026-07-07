// 동행 모집글 목록 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostQueryJpaRepository;
import com.sopt.nearby.companion.application.ReadCompanionPostsCommand;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSummary;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionPostQueryPort;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionPostQueryAdapter implements CompanionPostQueryPort {

    private final CompanionPostQueryJpaRepository repository;

    public CompanionPostQueryAdapter(final CompanionPostQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CompanionPostSummary> find(final ReadCompanionPostsCommand command) {
        return repository.find(
                        command.latitude(),
                        command.longitude(),
                        command.radiusMeters(),
                        command.placeCategory().name(),
                        command.placeId(),
                        command.sort().name()
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private CompanionPostSummary toSummary(final CompanionPostProjection row) {
        return new CompanionPostSummary(
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
