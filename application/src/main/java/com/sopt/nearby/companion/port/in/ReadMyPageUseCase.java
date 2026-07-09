// 마이페이지 조회 유스케이스 진입점을 정의하는 포트
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.ReadMyPageResult;

public interface ReadMyPageUseCase {

    ReadMyPageResult read(Long userId);
}
