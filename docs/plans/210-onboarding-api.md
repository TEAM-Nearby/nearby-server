# 온보딩 추가 기능 API와 앱 연동

- 관련 이슈: [#210](https://github.com/TEAM-Nearby/nearby-server/issues/210).
- 구현 브랜치: `feat/210`.
- 확정 정책: 비상 연락망은 선택 사항이며 사용자당 한 명만 등록한다. 동행 프로필을 건너뛰면 온보딩은 완료되지만 동행 화면은 프로필 등록 후 이용한다.

## API 계약

모든 요청에 Nearby 액세스 토큰이 필요하다. 사용자 ID는 인증 정보에서 가져오며 요청 본문에 받지 않는다.

| 메서드와 경로 | 동작 |
| --- | --- |
| `GET /api/onboarding` | 현재 온보딩 상태, 휴대폰 인증 여부, 프로필·비상 연락망 등록 여부를 조회한다. |
| `PUT /api/onboarding/emergency-contact` | 연락망 한 명을 저장한다. 이미 등록했다면 같은 항목을 수정한다. |
| `GET /api/onboarding/emergency-contact` | 본인의 연락망을 조회한다. 미등록이면 `data`는 `null`이다. |
| `POST /api/onboarding/companion-profiles/skip` | 휴대폰 인증 후 동행 프로필 설정을 건너뛴다. 본문은 없다. |
| `POST /api/onboarding/companion-profiles` | 기존 등록 API다. 건너뛴 후에도 같은 API로 등록할 수 있다. |

비상 연락망 저장 요청 예시는 다음과 같다. 이름은 공백만으로 구성할 수 없으며 최대 50자다. 전화번호는 구분자 없는 8~15자리 숫자이며 앞에 `+`를 붙일 수 있다.

```json
{
  "name": "홍길동",
  "phoneNumber": "01012345678"
}
```

비상 연락망을 등록하지 않고 프로필을 건너뛴 사용자의 상태 응답에서 `data`는 다음과 같다.

```json
{
  "onboardingStatus": "COMPLETED",
  "phoneVerified": true,
  "hasCompanionProfile": false,
  "hasEmergencyContact": false
}
```

기존 공개 상태 값 `STARTED`, `PHONE_VERIFIED`, `COMPLETED`는 유지한다. 애플·카카오 로그인 응답에 `hasCompanionProfile`을 추가해 완료 상태와 프로필 등록 여부를 구분한다. 휴대폰 재인증으로 프로필 등록·건너뛰기 상태가 초기화되지 않는다.

## 앱 처리 흐름

1. 로그인 응답과 `GET /api/onboarding`으로 현재 상태를 확인한다.
2. 비상 연락망을 입력하면 저장 API를 호출한다. 입력하지 않으면 별도 호출 없이 다음 단계로 이동한다.
3. 휴대폰 인증 후 프로필을 등록하거나 건너뛰기 API를 호출한다. 건너뛰기 재호출은 기존 상태를 유지한다.
4. `hasCompanionProfile=false`인 사용자가 동행 화면에 진입하면 등록 안내 모달을 표시한다. 서버의 `403 COMPANION_PROFILE_REQUIRED` 응답에도 같은 안내를 연결한다.
5. 모달에서 등록을 선택하면 기존 등록 화면·API를 사용한다. 성공 후 상태를 다시 조회하고 원래 화면으로 이동한다. 서버 접근 검사는 DB의 현재 상태를 확인하므로 기존 액세스 토큰을 그대로 사용할 수 있다.

접근 제한 대상은 동행 모집글, 프로필 조회, 요청, 매칭, 만남, 내가 작성한 모집글, 동행 요청 알림·결과 API다. 온보딩 미완료 사용자는 기존 `403 ONBOARDING_REQUIRED` 응답을 받는다.

마이페이지는 건너뛴 사용자도 조회할 수 있다. `hasCompanionProfile=false`, 프로필 속성은 `null`, 키워드 목록은 빈 배열, 활동 횟수는 0으로 반환한다. 앱은 이 응답을 프로필 등록 안내에 사용한다.

## 데이터와 검증 범위

사용자 행 잠금으로 연락망 저장과 온보딩 상태 변경의 동시 요청을 순서대로 처리한다. V25 마이그레이션은 `emergency_contact.user_id`에 유니크 제약을 추가한다. 기존 중복 행을 임의로 삭제하지 않으므로 배포 DB에 중복이 있다면 적용 전 정리가 필요하다.

통합 테스트는 H2에서 실제 Flyway 마이그레이션과 HTTP·DB 경계를 사용한다. 선택 등록, 사용자별 1명 제한, 입력·인증 오류, 동행 접근 제한, 건너뛰기 후 등록, 등록 실패 롤백, 동시 요청, Swagger 예시를 검증한다. 앱 모달 구현과 실제 PostgreSQL·배포 환경 검증은 별도다.

2026-10-01 로컬 검증 결과:

- `./gradlew test :bootstrap:bootJar` 통과. 현재 모듈 테스트 결과는 622개, 실패·오류·건너뜀은 0개다.
- 휴대폰 재인증 응답의 공개 상태 값을 통일한 뒤 `./gradlew :adapter-in:web:test :bootstrap:bootJar` 재검증 통과.
- Spring Modulith 경계 검증과 온보딩 통합 테스트 11개를 포함한다. CI와 배포는 실행하지 않았다.
