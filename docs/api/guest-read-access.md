# 비회원 조회 API

다음 세 API는 Authorization 헤더 없이 호출할 수 있다.

| 메서드 | 경로 | 비회원 동작 |
| --- | --- | --- |
| GET | `/api/companion-posts` | 온보딩 검사 없이 동행 모집글 목록을 조회한다. |
| GET | `/api/solo-dining/places` | 혼밥 목록을 조회하며 모든 `isFavorite`는 `false`다. |
| GET | `/api/solo-dining/places/{placeId}` | 식당 상세를 조회하며 `isFavorite`는 `false`다. |

## 인증과 응답

- 비회원 요청은 Authorization 헤더를 생략한다. 빈 Bearer 값이나 임의 토큰을 보내지 않는다.
- 유효한 액세스 토큰을 보내면 회원으로 처리한다. 혼밥 목록·상세에는 해당 회원의 실제 저장 여부를 반환한다.
- 로그인한 사용자의 동행 목록 조회에는 기존 온보딩 완료·동행 프로필 정책을 적용한다. 온보딩 미완료는 `403 ONBOARDING_REQUIRED`, 프로필 미등록은 `403 COMPANION_PROFILE_REQUIRED`를 받는다.
- 잘못되거나 만료된 액세스 토큰은 공개 경로에서도 `401 UNAUTHORIZED`를 반환한다.
- 요청 파라미터와 응답 필드는 기존과 같다. 세 API 모두 `latitude`, `longitude`가 필요하다. 잘못된 요청의 400과 없는 식당의 404 응답도 유지한다.
- Swagger의 세 GET operation은 익명 요청과 bearer 인증을 모두 허용하는 선택적 인증으로 표시한다.

## 요청 예시

```http
GET /api/companion-posts?latitude=48.86&longitude=2.34
GET /api/solo-dining/places?latitude=48.86&longitude=2.34&category=RESTAURANT
GET /api/solo-dining/places/123?latitude=48.86&longitude=2.34
```

혼밥 목록의 지도 경계 네 값도 기존과 동일하게 사용할 수 있다. 경계를 생략하면 반경 1km로 조회하고, 모두 전달하면 지정 영역에서 가까운 순으로 최대 100곳을 조회한다.

```http
GET /api/solo-dining/places?latitude=48.86&longitude=2.34&southLatitude=48.85&northLatitude=48.87&westLongitude=2.33&eastLongitude=2.35
```

## 로그인 전용 기능

즐겨찾기 등록·해제와 개인 저장 목록, 동행 작성·신청, 동행 글 상세는 계속 인증이 필요하다. 비회원이 저장이나 신청을 선택하면 앱에서 로그인 흐름으로 연결해야 한다.
