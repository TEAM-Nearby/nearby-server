// 동행 화면의 HTTP 진입점에 공통 프로필 접근 검사를 연결한다.
package com.sopt.nearby.companion.adapter.in.web.config;

import com.sopt.nearby.companion.port.in.RequireCompanionProfileUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CompanionAccessWebConfig implements WebMvcConfigurer {
    private final RequireCompanionProfileUseCase requireProfile;

    public CompanionAccessWebConfig(final RequireCompanionProfileUseCase requireProfile) {
        this.requireProfile = requireProfile;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response,
                                     final Object handler) {
                if (handler instanceof HandlerMethod) {
                    requireProfile.requireProfile(Long.valueOf(request.getUserPrincipal().getName()));
                }
                return true;
            }
        }).addPathPatterns(
                "/api/companion-posts", "/api/companion-posts/**",
                "/api/companion-profiles", "/api/companion-profiles/**",
                "/api/companion-requests", "/api/companion-requests/**",
                "/api/companion-matches", "/api/companion-matches/**",
                "/api/companion-meetings", "/api/companion-meetings/**",
                "/api/users/me/recruitment-posts", "/api/users/me/recruitment-posts/**",
                "/api/users/me/companion-requests", "/api/users/me/companion-requests/**"
        );
    }
}
