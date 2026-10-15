// 동행 모집글 목록의 참여자 프로필 조회 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

public interface CompanionPostParticipantProjection {

    Long getPostId();

    Long getUserId();

    String getProfileImageUrl();
}
