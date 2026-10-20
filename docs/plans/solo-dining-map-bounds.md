# 혼밥지도 화면 영역 조회

관련 이슈: [#239](https://github.com/TEAM-Nearby/nearby-server/issues/239).
작업 브랜치: `feat/239`. 기준 커밋: `develop`의 `f3badb4f`.

지도에서 보이는 영역을 넓혀도 기존 API는 사용자 위치 반경 1km를 조회했다.
앱이 지도 경계를 전달하면 서버가 해당 영역의 혼밥지도 등록 장소를 조회하도록 한다.

## 요청

`GET /api/solo-dining/places`에 다음 선택 파라미터를 추가한다.

| 파라미터 | 의미 |
| --- | --- |
| `southLatitude` | 화면 남쪽 위도. -90 이상 90 이하. |
| `northLatitude` | 화면 북쪽 위도. 남쪽 위도보다 커야 한다. |
| `westLongitude` | 화면 서쪽 경도. -180 이상 180 이하. |
| `eastLongitude` | 화면 동쪽 경도. 서쪽 경도와 달라야 한다. |

네 값을 모두 생략하면 기존처럼 사용자 위치 반경 1km를 조회한다.
하나라도 보내면 나머지 경계도 필요하며, 누락·빈 문자열·잘못된 좌표는 HTTP 400과
`VALIDATION_ERROR`로 응답한다. 서쪽 경도가 동쪽 경도보다 크면 날짜변경선을 지나는 영역이다.
경계 위의 장소도 포함한다.

`latitude`, `longitude`는 계속 사용자 위치다. 검색 영역과 별개로 `distanceMeters` 계산과
가까운 순 정렬에 사용한다. 사용자 위치가 화면 밖에 있어도 화면 영역으로 조회할 수 있다.
`category`, 복수 분류, 즐겨찾기, 응답 구조와 혼밥지도 등록 조건은 유지한다.

파리에서 기존 요청은 다음과 같다.

```http
GET /api/solo-dining/places?latitude=48.8566&longitude=2.3522&category=RESTAURANT
```

지도 영역을 지정하는 요청 예시는 다음과 같다. 경계값은 실제 앱 화면에서 얻어야 한다.

```http
GET /api/solo-dining/places?latitude=48.8566&longitude=2.3522&category=RESTAURANT&southLatitude=48.83&northLatitude=48.89&westLongitude=2.30&eastLongitude=2.40
```

## 앱 연동

1. 지도의 최초 표시와 이동·확대·축소가 끝났을 때 화면 영역의 경계를 읽는다.
2. 네 경계와 사용자 위치, 선택된 카테고리로 목록을 다시 요청한다.
3. 연속 조작 중 요청이 겹치면 이전 요청을 취소하거나 요청 순번을 비교해 최신 응답만 반영한다.
4. 응답의 장소 목록으로 화면에 표시할 마커와 목록을 갱신한다.

연속 이벤트가 발생하는 지도 SDK에서는 조작 종료 이벤트를 사용하거나 짧은 지연으로 요청을
합친다. 카테고리가 바뀔 때도 현재 지도 영역을 함께 전달한다.
서버 배포 후 앱에서 새 파라미터를 보내야 지도 조작이 조회 결과에 반영된다.

## 변경 파일

- HTTP 요청·문서: `SoloDiningPlaceApi.java`, `SoloDiningPlaceController.java`, `SoloDiningPlacesRequest.java`.
- 조회 조건·검증: `ReadSoloDiningPlacesCommand.java`, `ReadSoloDiningPlacesService.java`, `SoloDiningMapBounds.java`, `SoloDiningPlaceQueryPort.java`.
- DB 조회: `SoloDiningPlaceQueryAdapter.java`, `SoloDiningPlaceQueryJpaRepository.java`.
- 테스트: `SoloDiningPlaceControllerTest.java`, `SoloDiningPlaceQueryAdapterTest.java`, `ReadSoloDiningPlacesServiceTest.java`, `ReadSoloDiningPlaceServiceTest.java`, `SoloDiningPlaceRegistrationFlowTest.java`, `PlaceUseCaseConfigTest.java`.
- 연동 문서: `docs/plans/solo-dining-map-bounds.md`.

## 검증 결과

2026-10-20 Java 21에서 다음 검증을 실행했다. 관련 테스트 49개가 통과했고 실행 JAR 빌드도 성공했다.

```bash
./gradlew :application:test --tests '*ReadSoloDiningPlacesServiceTest' \
  :adapter-in:web:test --tests '*SoloDiningPlaceControllerTest' \
  :adapter-out:persistence:test --tests '*SoloDiningPlaceQueryAdapterTest'

./gradlew :bootstrap:test --tests '*SoloDiningPlaceRegistrationFlowTest' \
  --tests '*PlaceUseCaseConfigTest' :bootstrap:bootJar
```

이후 최신 `develop`에서 `feat/239`를 생성하고 `./gradlew test :bootstrap:bootJar`를
실행했다. 전체 테스트 803개가 실패·오류·건너뜀 없이 통과했고 실행 JAR 빌드도 성공했다.

Supabase에 변경된 SQL을 읽기 전용으로 실행했을 때 파리 좌표 `(48.8566, 2.3522)`의
`RESTAURANT` 조회는 기존 1km 조건에서 5곳, 위 요청 예시의 지도 영역에서 37곳이었다.
DB 데이터 변경, 서버 배포와 실제 앱 연동 검증은 수행하지 않았다.
