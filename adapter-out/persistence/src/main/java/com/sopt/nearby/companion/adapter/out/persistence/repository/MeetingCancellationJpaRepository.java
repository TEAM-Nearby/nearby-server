// 미팅 취소 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCancellationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingCancellationJpaRepository extends JpaRepository<MeetingCancellationEntity, Long> {
}
