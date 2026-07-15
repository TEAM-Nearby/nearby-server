# 혼밥 즐겨찾기 목록 이미지 URL 제공 설계

## 목표

`GET /api/solo-dining/favorites`의 각 즐겨찾기에 브라우저가 표시 가능한 `imageUrl`을 추가한다. 기존 `photoReference`는 유지한다.

## 결정

- `photoReference`를 대체하지 않고 `imageUrl`을 추가한다.
- `ReadSoloDiningFavoritesService`가 기존 `ResolvePlaceImageUseCase`로 사진 리소스를 실제 URL로 해석한다.
- 일반 혼밥 목록과 같은 가상 스레드 병렬 처리로 목록 순서를 보존한다.
- 사진이 없거나 이미지 조회 포트가 빈 결과를 반환하면 기존 이미지 해석기의 기본 이미지 fallback을 사용한다. 유스케이스에서 발생한 `BusinessException`은 원인을 보존해 전파한다.
- 웹 응답과 Swagger 예시에 `imageUrl`을 추가한다.

## 검증 기준

- 각 즐겨찾기의 `googlePlaceId`와 `photoReference`가 이미지 해석기로 전달된다.
- 결과는 `photoReference`와 `imageUrl`을 모두 포함한다.
- 병렬 해석, 입력 순서 보존, `BusinessException` 원인 복원이 테스트된다.
- 애플리케이션, 웹, bootstrap 및 전체 Gradle 테스트가 통과한다.
