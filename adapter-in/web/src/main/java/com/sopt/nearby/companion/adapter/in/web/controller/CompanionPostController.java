// 동행 모집글 목록 조회 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CreateCompanionPostRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.request.NearbyCompanionPostsRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreatedCompanionPostResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.NearbyCompanionPostsResponse;
import com.sopt.nearby.companion.port.in.CreateCompanionPostUseCase;
import com.sopt.nearby.companion.port.in.ReadNearbyCompanionPostsUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-posts")
public class CompanionPostController implements CompanionPostApi {

    private static final String DEFAULT_RADIUS_METERS = "1000";
    private static final String DEFAULT_PLACE_CATEGORY = "ALL";
    private static final String DEFAULT_SORT = "LATEST";

    private final ReadNearbyCompanionPostsUseCase readNearbyCompanionPostsUseCase;
    private final CreateCompanionPostUseCase createCompanionPostUseCase;

    public CompanionPostController(
            final ReadNearbyCompanionPostsUseCase readNearbyCompanionPostsUseCase,
            final CreateCompanionPostUseCase createCompanionPostUseCase
    ) {
        this.readNearbyCompanionPostsUseCase = readNearbyCompanionPostsUseCase;
        this.createCompanionPostUseCase = createCompanionPostUseCase;
    }

    @Override
    @GetMapping
    public CommonResponse<NearbyCompanionPostsResponse> getPosts(
            @RequestParam(required = false) final String latitude,
            @RequestParam(required = false) final String longitude,
            @RequestParam(defaultValue = DEFAULT_RADIUS_METERS) final String radiusMeters,
            @RequestParam(defaultValue = DEFAULT_PLACE_CATEGORY) final String placeCategory,
            @RequestParam(defaultValue = DEFAULT_SORT) final String sort,
            final Principal principal
    ) {
        NearbyCompanionPostsRequest request = new NearbyCompanionPostsRequest(
                latitude,
                longitude,
                radiusMeters,
                placeCategory,
                sort
        );

        return CommonResponse.success(
                CompanionSuccessCode.COMPANION_POSTS_FOUND,
                NearbyCompanionPostsResponse.from(readNearbyCompanionPostsUseCase.read(
                        request.toCommand(Long.valueOf(principal.getName()))
                ))
        );
    }

    @Override
    @PostMapping
    public CommonResponse<CreatedCompanionPostResponse> createPost(
            @RequestBody(required = false) final CreateCompanionPostRequest request,
            final Principal principal
    ) {
        return CommonResponse.success(
                CompanionSuccessCode.COMPANION_POST_CREATED,
                CreatedCompanionPostResponse.from(createCompanionPostUseCase.create(
                        request == null ? null : request.toCommand(Long.valueOf(principal.getName()))
                ))
        );
    }
}
