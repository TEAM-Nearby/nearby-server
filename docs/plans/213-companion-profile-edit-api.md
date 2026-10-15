# 마이페이지 동행 프로필 수정 API

- 관련 이슈: [#213](https://github.com/TEAM-Nearby/nearby-server/issues/213).
- 구현 브랜치: `feat/213`.
- 확정 정책: 닉네임·한줄소개·프로필 이미지·여행 스타일을 수정한다. 성별·출생연도는 유지하며 소개와 이미지는 삭제할 수 있다.

## API 계약

Nearby 액세스 토큰이 필요하다. 사용자 ID는 인증 정보에서만 가져온다.

| 메서드와 경로 | 동작 |
| --- | --- |
| `GET /api/users/me/companion-profile` | 수정 화면의 초기값을 조회한다. |
| `PUT /api/users/me/companion-profile` | 수정 가능한 네 항목을 전체 교체하고 저장 결과를 반환한다. |

응답 `data`는 `profileId`, `nickname`, `gender`, `birthYear`, `intro`, `profileImageUrl`, `travelStyleKeywords`를 포함한다. `gender`와 `birthYear`는 조회 전용이다. 여행 스타일의 순서는 의미가 없다.

```json
{
  "nickname": "여행친구",
  "intro": "함께 걸어요",
  "profileImageUrl": "https://example.com/profile.png",
  "travelStyleKeywords": ["EXTROVERTED", "CAFE_TOUR"]
}
```

| 항목 | 입력 규칙 |
| --- | --- |
| `nickname` | 필수, 최대 15자, 공백만으로 구성할 수 없다. 본인의 기존 닉네임은 재사용할 수 있다. |
| `intro` | 선택, 최대 50자. 생략·`null`·빈 문자열·공백만 입력하면 삭제한다. |
| `profileImageUrl` | 선택, 최대 255자의 HTTP(S) URL. 생략·`null`·빈 문자열·공백만 입력하면 삭제한다. |
| `travelStyleKeywords` | 필수, 한 개 이상의 유효한 키워드. 중복과 `null` 원소를 허용하지 않는다. 기존 목록을 전체 교체한다. |

문자열 앞뒤 공백을 제거하고 선택 항목의 삭제 여부를 정한 뒤 길이를 검사한다. 공백만 있는 소개·이미지는 원문이 길이 제한을 초과해도 삭제 시 DB의 `null`로 저장한다. 정규화 후에도 길이를 초과하면 `400 INVALID_COMPANION_PROFILE_UPDATE`를 반환한다. 같은 요청을 반복해도 프로필이나 스타일 연결 행이 늘어나지 않는다.

성별·출생연도·프로필 ID·사용자 ID·매너 점수·후기 수·프로필 상태·활동 내역·휴대폰 인증·온보딩 상태를 변경하지 않는다. 본문에 사용자 ID나 수정 불가 항목을 추가해도 수정 대상으로 사용하지 않는다.

## 오류와 앱 연동

| HTTP 상태 | 코드 | 앱 처리 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR`, `INVALID_COMPANION_PROFILE_UPDATE`, `BAD_REQUEST` | 입력 내용과 JSON 형식을 확인한다. |
| 401 | 인증 오류 | 유효한 액세스 토큰으로 요청한다. |
| 403 | `ONBOARDING_REQUIRED` | 온보딩을 완료한다. |
| 403 | `COMPANION_PROFILE_REQUIRED` | 미등록 상태다. 기존 `POST /api/onboarding/companion-profiles`로 등록한다. |
| 403 | `FORBIDDEN_INACTIVE_COMPANION_PROFILE` | 비활성 프로필은 조회·수정할 수 없다. 수정으로 활성화하지 않는다. |
| 404 | `USER_NOT_FOUND` | 인증 정보에 해당하는 사용자를 찾을 수 없다. |
| 409 | `DUPLICATE_NICKNAME` | 다른 닉네임을 입력한다. 동시에 같은 닉네임을 저장해도 같은 오류를 반환한다. |

이미지 교체 시 기존 `POST /api/onboarding/profile-images/presigned-url`로 업로드 URL을 발급받고, 업로드가 끝난 이미지 URL을 수정 요청에 포함한다. 이미지 삭제는 프로필의 URL 연결만 제거하며 S3 객체를 삭제하지 않는다. 기존 이미지 객체 정리는 이 이슈의 범위에 포함하지 않는다.

## 저장과 검증

본인 프로필 행에 쓰기 잠금을 잡아 같은 사용자의 동시 수정을 순서대로 처리한다. 프로필 변경과 스타일 전체 교체는 하나의 트랜잭션으로 저장한다. 스타일 저장이 실패하면 닉네임·소개·이미지와 기존 스타일을 모두 복구한다. 기존 닉네임 유니크 제약으로 다른 사용자의 중복 저장도 차단하므로 스키마 변경은 없다.

본인 프로필 조회는 `REPEATABLE_READ` 읽기 전용 트랜잭션을 사용한다. 프로필과 스타일 조회 사이에 수정이 커밋되어도 PostgreSQL의 동일한 스냅샷에서 읽으므로 이전 프로필과 새로운 스타일이 섞이지 않는다. Swagger의 조회·수정 403·404 예시는 웹 계층에서 정의하며 실제 오류 응답과 동일한지 통합 테스트로 확인한다.

단위 테스트는 입력 경계값과 삭제 규칙을 검증한다. 통합 테스트는 실제 HTTP·DB 경계에서 본인 소유권, 수정 불가 정보 유지, 기존 조회 API 반영, 반복 수정, 중복 닉네임, 접근 제한, 잘못된 입력, DB 실패 롤백, 동시 요청과 Swagger 계약을 검증한다. 임시 PostgreSQL에서 같은 통합 테스트를 실행하려면 아래 환경 변수를 설정한다. 테스트는 전용 DB에 데이터를 생성하고 롤백 검증용 제약조건을 잠시 추가하므로 개발·운영 DB를 사용하지 않는다.

```bash
ISSUE213_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55413/postgres \
ISSUE213_TEST_DB_USERNAME=nearby213 \
./gradlew :bootstrap:test --tests '*MyCompanionProfileFlowTest' --rerun-tasks
```

2026-10-03 로컬 검증 결과:

- `./gradlew test :bootstrap:bootJar` 통과. 현재 모듈 테스트 총 644개, 실패·오류·건너뜀 0개다. Spring Modulith 경계 검증을 포함한다.
- 임시 PostgreSQL 14.18에서 `MyCompanionProfileFlowTest` 13개 통과. 동일 사용자 동시 수정, 서로 다른 사용자의 닉네임 충돌과 스타일 저장 실패 시 전체 롤백을 확인했다.
- 두 조회 사이에 수정 커밋을 완료시키는 동시성 테스트로 프로필·스타일의 동일 스냅샷 조회와 다음 요청의 최신 값 조회를 확인했다.
- HTTP 입력 정규화 후 길이 경계, 긴 공백 선택 항목 삭제, 조회·수정 API의 실제 403·404 응답과 Swagger 예시 일치를 검증했다.
- `git diff --check` 통과.

위 결과는 로컬 검증 기준이다. 앱 화면 연동과 실제 S3 업로드, 배포 결과는 별도 검증 대상이다.
