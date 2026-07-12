// 동행 모집글 목록 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostParticipantProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostQueryJpaRepository;
import com.sopt.nearby.companion.application.ReadCompanionPostsCommand;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSummary;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionPostQueryPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionPostQueryAdapter implements CompanionPostQueryPort {

    private final CompanionPostQueryJpaRepository repository;

    public CompanionPostQueryAdapter(final CompanionPostQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CompanionPostSummary> find(final ReadCompanionPostsCommand command) {
        List<CompanionPostProjection> rows = repository.find(
                        command.latitude(),
                        command.longitude(),
                        command.radiusMeters(),
                        command.placeCategory().name(),
                        command.placeId(),
                        command.sort().name()
                );
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<CompanionPostSummary.Participant>> participantsByPostId = repository
                .findParticipantsByPostIds(rows.stream().map(CompanionPostProjection::getPostId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        CompanionPostParticipantProjection::getPostId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                participant -> new CompanionPostSummary.Participant(
                                        participant.getUserId(),
                                        participant.getProfileImageUrl()
                                ),
                                Collectors.toList()
                        )
                ));

        return rows.stream()
                .map(row -> toSummary(row, participantsByPostId.getOrDefault(row.getPostId(), List.of())))
                .toList();
    }

    private CompanionPostSummary toSummary(
            final CompanionPostProjection row,
            final List<CompanionPostSummary.Participant> participants
    ) {
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
                row.getCreatedAt(),
                participants
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
