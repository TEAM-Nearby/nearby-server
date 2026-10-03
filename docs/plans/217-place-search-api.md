# 동행글 작성 장소 검색 API

- 이슈: [#217](https://github.com/TEAM-Nearby/nearby-server/issues/217).
- 기준 브랜치: `feat/217`, `develop`의 `abeeb32`에서 시작.
- 서버 구현: `GET /api/companion-places/search`.
- 확인 날짜: 2026-10-03, Asia/Seoul.

## 구현 범위

기존 서버에는 Google Nearby Search와 장소 상세 조회가 있지만 장소 이름을 받는 검색 API는 없었다. 새 API는 Google Places Text Search (New)를 호출하며, 검색어를 임의로 번역하거나 특정 장소의 별칭을 하드코딩하지 않는다.

앱 코드가 제공되지 않아 기존 앱의 SDK·검색 파라미터와 검색 실패 원인은 확인하지 못했다. 이 API는 앱에서 연결해야 검색 화면에 반영된다. 앱 수정·배포와 화면 검증은 아직 수행하지 않았다.

## 요청

JWT 액세스 토큰, 온보딩 완료, 활성 동행 프로필이 필요하다. 기존 동행 접근 인터셉터로 검사하며, 접근 거부 시 Google을 호출하지 않는다.

```http
GET /api/companion-places/search?query=시우다드%20콘달&latitude=41.3874&longitude=2.1686
Authorization: Bearer <access-token>
```

실제 클라이언트는 검색어와 토큰을 URL 인코딩해야 한다.

| 파라미터 | 계약 |
| --- | --- |
| `query` | 필수. 앞뒤 공백 제거와 연속 공백 정리 후 1~200 Unicode 코드 포인트. |
| `latitude`, `longitude` | 필수. 선택 도시 또는 지도 중심 좌표. 위도 -90~90, 경도 -180~180. |
| `radiusMeters` | 위치 우선순위 반경. 기본 20000, 허용 1~50000m. |
| `languageCode` | 기본 `ko`. `ko`, `en`, `es`, `fr`, `ja`, `de`, `it`, `pt` 지원. |
| `pageSize` | 기본 20, 허용 1~20. |
| `pageToken` | 이전 응답의 `nextPageToken`. 최대 4096자. 첫 요청에서는 생략. |

선택한 해외 도시의 중심 좌표를 전달해야 한다. 한국에서 검색 중이라는 이유로 기기의 한국 좌표를 보내지 않는다. 서버는 도시 목록을 새로 하드코딩하지 않고 좌표를 받으므로 바르셀로나 사례도 검색할 수 있다. 기존 `CompanionCity`의 도시·시간대 판별 범위는 변경하지 않는다.

`languageCode`는 표시 언어이고, `locationBias`는 검색 위치의 우선순위다. 반경 밖 결과도 나올 수 있으며 검색어에 명시된 다른 지역이 우선할 수 있다. `regionCode=KR`이나 식당 종류 필터를 강제로 붙이지 않는다. 다음 페이지 요청에는 검색어·좌표·반경·언어 등 처음과 같은 조건을 보내며, 조건이 바뀌면 토큰을 버린다. 외부 토큰이 만료되거나 Google이 조건 불일치로 거부하면 502가 반환되므로 첫 페이지부터 다시 검색한다.

## 응답과 동행글 연결

```json
{
  "status": 200,
  "code": "PLACES_SEARCHED",
  "message": "장소 검색에 성공했습니다.",
  "data": {
    "places": [{
      "googlePlaceId": "ChIJmSmV-_KipBIR1rXbKL9Yhp4",
      "name": "시우다드 콘달",
      "address": "Rambla de Catalunya, 18, Eixample, 08007 Barcelona, 스페인",
      "latitude": 41.388854699999996,
      "longitude": 2.1670032999999997,
      "category": "PUB",
      "attributions": []
    }],
    "nextPageToken": null
  }
}
```

결과가 없으면 `places: []`, 마지막 페이지면 `nextPageToken: null`이다. 주소가 없는 장소는 `address: null`이다. Google 응답에 ID·이름·유효한 좌표가 없거나 응답 형태가 잘못되면 빈 결과로 숨기지 않고 502를 반환한다. 중복 Google Place ID는 응답 순서를 유지하며 한 번만 반환한다.

카테고리는 Google `primaryType`을 먼저 사용하고 `types`로 보완한다. `RESTAURANT`, `CAFE`, `PUB`, `MUSEUM`, `PHOTO_SPOT`, `OTHER`는 기존 작성 API에 전달 가능한 값이다. 실제 시우다드 콘달의 Google 기본 유형은 `bar`이므로 `PUB`으로 반환됐다.

앱은 선택한 결과의 `googlePlaceId`, `name`, `address`, `latitude`, `longitude`, `category`를 기존 `POST /api/companion-posts` 요청의 `place`에 그대로 전달한다. `attributions`는 검색 화면에 표시할 출처 정보이며 작성 요청 필드가 아니다. 기존 장소 저장 흐름이 Google Place ID로 장소를 재사용하고 상세 조회에 같은 ID·주소·좌표를 반환한다. DB 좌표는 기존 소수점 8자리 정밀도로 저장한다.

## 오류·설정·저장

| HTTP | 코드 | 의미 |
| --- | --- | --- |
| 400 | `INVALID_PLACE_SEARCH_REQUEST` | 검색어, 좌표, 언어, 반경, 페이지 조건 오류. |
| 401 | 기존 인증 오류 | 유효한 인증이 없음. |
| 403 | `ONBOARDING_REQUIRED` | 온보딩 미완료. |
| 403 | `COMPANION_PROFILE_REQUIRED` | 동행 프로필 없음·건너뜀·비활성. |
| 404 | `USER_NOT_FOUND` | 인증 주체에 해당하는 사용자가 없음. |
| 502 | `PLACE_SEARCH_FAILED` | API 키 누락, 외부 비정상 응답·통신 실패·잘못된 응답. |
| 503 | `PLACE_SEARCH_RATE_LIMITED` | Google의 429 응답. |
| 504 | `PLACE_SEARCH_TIMEOUT` | Google 연결·응답 제한 시간 초과. |

- 기존 `GOOGLE_PLACES_API_KEY`를 사용한다. 키는 요청 헤더로만 전달하며 출력하지 않는다.
- 검색 전용 제한 시간은 `GOOGLE_PLACES_SEARCH_TIMEOUT_MS`, 기본 5000ms다. 실제 첫 호출이 약 3.6~3.8초 소요돼 기존 주변 검색의 1000ms 설정과 분리했다.
- 필요한 필드만 요청한다. 사진·리뷰·평점 추가 호출과 자동 재시도는 없다.
- 새 검색 결과는 Redis·DB에 저장하지 않는다. 장소 선택 후 작성할 때만 기존 저장 흐름을 사용한다. DB 마이그레이션은 없다.
- 앱은 검색 실행 시 호출하고, 입력 중 자동 검색을 연결한다면 중복 요청·오래된 응답 표시를 방지해야 한다.
- 앱은 Google Maps 및 반환된 제3자 출처를 표시한다. 지도 표시 방식과 기존 장소 데이터 보관 정책은 아래 Google 문서를 따른다.

## 검증 기록

### 실제 Google Places

2026-10-03에 프로젝트의 기존 로컬 키로 호출했다. 바르셀로나 중심 `41.3874, 2.1686`, 반경 20000m, 표시 언어 `ko`를 사용했다. 아래 세 건은 새 Java 어댑터 자체를 실행해 확인했다.

| 검색어 | 결과 | 소요 시간 |
| --- | --- | --- |
| `시우다드 콘달` | 목표 ID `ChIJmSmV-_KipBIR1rXbKL9Yhp4`. | 약 3760ms. |
| `Ciutat Comtal` | 같은 목표 ID. | 약 203ms. |
| `ciutat comdal` | 같은 목표 ID. | 약 199ms. |

같은 조건의 별도 HTTP 확인에서 `languageCode=en`의 한국어 검색도 같은 ID를 반환했다. 반면 `Ciudad Condal`은 바르셀로나 도시 ID `ChIJ5TCOcRaYpBIRCmZHTz37sEQ`를 반환했다. 모든 별칭·오타를 인식하거나 반경 밖 장소를 제외한다고 보장하지 않는다. 기존 앱의 변경 전 결과와 비교한 검증은 아니다.

### 자동 테스트

- 검색어 정규화, 길이·좌표·반경·언어·페이지 경계 검증.
- 로컬 HTTP 서버를 통한 실제 요청 본문·헤더·필드 마스크, 언어와 위치 전달, 분류·출처·페이지 토큰 변환 검증.
- 빈 검색 결과와 외부 오류·429·시간 초과·잘못된 JSON 및 필수 필드 누락 구분.
- HTTP 인증·온보딩·동행 프로필 접근 정책과 외부 호출 차단.
- 검색 결과 선택 → 동행글 두 번 작성 → 상세 조회에서 ID·주소·좌표·분류 유지 및 장소 중복 저장 방지.
- Swagger 성공·오류 응답 계약 검증.

`./gradlew test :bootstrap:bootJar` 통과. 현재 Gradle 모듈의 테스트 700개 중 실패·오류·건너뜀은 0개이며, 신규 테스트 39개와 Spring Modulith 경계 검증을 포함한다. 통합 테스트는 H2 PostgreSQL 모드에서 수행했다. `git diff --check`도 통과했다. 이 브랜치의 CI·배포·앱 화면 검증은 아직 수행하지 않았다.

## 참고 문서

- [Google Text Search (New)](https://developers.google.com/maps/documentation/places/web-service/text-search).
- [Google Text Search 요청 계약](https://developers.google.com/maps/documentation/places/web-service/reference/rest/v1/places/searchText).
- [Google Places 표시·저장 정책](https://developers.google.com/maps/documentation/places/web-service/policies).
