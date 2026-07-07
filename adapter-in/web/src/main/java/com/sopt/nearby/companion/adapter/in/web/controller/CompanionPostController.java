// 동행 모집글 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CreateCompanionPostRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CompanionPostsRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionPostDetailResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreatedCompanionPostResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionPostsResponse;
import com.sopt.nearby.companion.application.ReadCompanionPostDetailCommand;
import com.sopt.nearby.companion.port.in.CreateCompanionPostUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionPostDetailUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionPostsUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private final ReadCompanionPostsUseCase readCompanionPostsUseCase;
    private final CreateCompanionPostUseCase createCompanionPostUseCase;
    private final ReadCompanionPostDetailUseCase readCompanionPostDetailUseCase;

    public CompanionPostController(
            final ReadCompanionPostsUseCase readCompanionPostsUseCase,
            final CreateCompanionPostUseCase createCompanionPostUseCase,
            final ReadCompanionPostDetailUseCase readCompanionPostDetailUseCase
    ) {
        this.readCompanionPostsUseCase = readCompanionPostsUseCase;
        this.createCompanionPostUseCase = createCompanionPostUseCase;
        this.readCompanionPostDetailUseCase = readCompanionPostDetailUseCase;
    }

    @Override
    @GetMapping
    public CommonResponse<CompanionPostsResponse> getPosts(
            @RequestParam(required = false) final String latitude,
            @RequestParam(required = false) final String longitude,
            @RequestParam(defaultValue = DEFAULT_RADIUS_METERS) final String radiusMeters,
            @RequestParam(defaultValue = DEFAULT_PLACE_CATEGORY) final String placeCategory,
            @RequestParam(required = false) final String placeId,
            @RequestParam(defaultValue = DEFAULT_SORT) final String sort,
            final Principal principal
    ) {
        CompanionPostsRequest request = new CompanionPostsRequest(
                latitude,
                longitude,
                radiusMeters,
                placeCategory,
                placeId,
                sort
        );

        return CommonResponse.success(
                CompanionSuccessCode.COMPANION_POSTS_FOUND,
                CompanionPostsResponse.from(readCompanionPostsUseCase.read(
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

    @Override
    @GetMapping("/{postId}")
    public CommonResponse<CompanionPostDetailResponse> getPost(
            @PathVariable final Long postId,
            final Principal principal
    ) {
        return CommonResponse.success(
                CompanionSuccessCode.COMPANION_POST_FOUND,
                CompanionPostDetailResponse.from(readCompanionPostDetailUseCase.read(
                        new ReadCompanionPostDetailCommand(
                                Long.valueOf(principal.getName()),
                                postId
                        )
                ))
        );
    }
}
