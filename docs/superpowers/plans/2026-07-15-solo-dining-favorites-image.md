# 혼밥 즐겨찾기 목록 이미지 URL 제공 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 즐겨찾기 혼밥 맛집 목록의 각 항목에 표시 가능한 대표 이미지 URL을 제공한다.

**Architecture:** 즐겨찾기 조회 서비스가 기존 `ResolvePlaceImageUseCase`로 사진을 병렬 해석하고, 별도 애플리케이션 결과 모델의 `imageUrl`로 전달한다. 웹 어댑터는 기존 `photoReference`와 추가된 `imageUrl`을 함께 직렬화한다.

**Tech Stack:** Java 21, Spring Boot 3.5, JUnit 5, MockMvc, Gradle.

## Global Constraints

- 기존 `photoReference` 필드는 제거하거나 의미를 바꾸지 않는다.
- 사진 미보유 또는 이미지 조회 포트의 빈 결과에는 기존 기본 이미지 fallback을 사용하고, `BusinessException`은 원인을 보존해 전파한다.
- 이미지 조회는 가상 스레드 기반 병렬 처리와 입력 순서 보존을 제공한다.
- 애플리케이션은 어댑터 모듈에 의존하지 않는다.
- 커밋은 사용자 요청이 있을 때만 수행한다.

---

### Task 1: 이미지가 포함된 즐겨찾기 애플리케이션 결과 만들기

**Files:**
- Modify: `application/src/main/java/com/sopt/nearby/place/application/SoloDiningFavoritesResult.java`
- Modify: `application/src/main/java/com/sopt/nearby/place/application/ReadSoloDiningFavoritesService.java`
- Modify: `application/src/test/java/com/sopt/nearby/place/application/ReadSoloDiningFavoritesServiceTest.java`

- [x] 즐겨찾기 요약과 해석된 `imageUrl`을 함께 담는 결과 모델을 추가한다.
- [x] 각 즐겨찾기를 가상 스레드에서 해석하고 순서대로 수집한다.
- [x] 해석 명령 전달, 병렬 실행, 순서 보존, `BusinessException` 원인 복원을 검증한다.

### Task 2: Spring 조립과 HTTP 응답 계약을 갱신한다

**Files:**
- Modify: `bootstrap/src/main/java/com/sopt/nearby/place/config/PlaceUseCaseConfig.java`
- Modify: `bootstrap/src/test/java/com/sopt/nearby/place/config/PlaceUseCaseConfigTest.java`
- Modify: `adapter-in/web/src/main/java/com/sopt/nearby/place/adapter/in/web/dto/response/SoloDiningFavoritesResponse.java`
- Modify: `adapter-in/web/src/main/java/com/sopt/nearby/place/adapter/in/web/controller/SoloDiningFavoriteApi.java`
- Modify: `adapter-in/web/src/test/java/com/sopt/nearby/place/adapter/in/web/controller/SoloDiningFavoriteControllerTest.java`

- [x] Spring 빈에 기존 `ResolvePlaceImageUseCase`를 주입한다.
- [x] `favorites[].imageUrl`을 `photoReference` 뒤에 추가한다.
- [x] Swagger 예시와 MockMvc JSON 계약 테스트를 갱신한다.

### Task 3: 회귀를 검증하고 작업 기록을 갱신한다

- [x] `./gradlew :application:test :adapter-in:web:test :bootstrap:test`를 실행한다.
- [x] `./gradlew test`를 실행한다.
- [x] `git diff --check`로 공백 오류를 점검한다.
