# 작업 맥락 기록

## 2026-07-15 feat/175

- 즐겨찾기 목록은 Google Photo 리소스 이름을 `photoReference`로 반환하고 있었다.
- 일반 목록과 일관되게 즐겨찾기 목록에도 `imageUrl`을 추가하고, `photoReference`는 호환성을 위해 유지했다.
- `ReadSoloDiningFavoritesService`는 기존 `ResolvePlaceImageUseCase`를 병렬 호출해 빈 이미지 조회 결과의 기본 이미지 fallback을 재사용하고, `BusinessException`은 원인을 보존해 전파한다.
- `./gradlew :application:test :adapter-in:web:test :bootstrap:test`가 `BUILD SUCCESSFUL`로 완료됐다.
- `./gradlew test`가 `BUILD SUCCESSFUL`로 완료됐다.
- `git diff --check`가 공백 오류 없이 통과했다.
