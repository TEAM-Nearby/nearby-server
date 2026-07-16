// 동행 신청 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;

public interface CompanionApplicationRepository extends DomainRepository<CompanionApplication, Long> {

    boolean existsByPostIdAndApplicantUserId(Long postId, Long applicantUserId);

    long countAcceptedByPostId(Long postId);

    boolean updateStatusIfPending(Long applicationId, CompanionApplicationStatus status, String rejectionReason);
}
