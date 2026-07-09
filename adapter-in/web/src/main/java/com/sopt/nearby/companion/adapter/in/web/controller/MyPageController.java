// 마이페이지 조회 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.response.MyPageResponse;
import com.sopt.nearby.companion.port.in.ReadMyPageUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/mypage")
public class MyPageController implements MyPageApi {

    private final ReadMyPageUseCase readMyPageUseCase;

    public MyPageController(final ReadMyPageUseCase readMyPageUseCase) {
        this.readMyPageUseCase = readMyPageUseCase;
    }

    @Override
    @GetMapping
    public CommonResponse<MyPageResponse> getMyPage(final Principal principal) {
        return CommonResponse.success(
                CompanionSuccessCode.READ_MY_PAGE,
                MyPageResponse.from(readMyPageUseCase.read(Long.valueOf(principal.getName())))
        );
    }
}
