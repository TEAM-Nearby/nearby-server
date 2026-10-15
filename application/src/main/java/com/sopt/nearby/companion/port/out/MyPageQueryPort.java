// 마이페이지 조회에 필요한 프로필과 활동 요약을 조회하는 포트
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.profile.MyPageProfile;
import java.util.Optional;

public interface MyPageQueryPort {

    Optional<MyPageProfile> findByUserId(Long userId);
}
