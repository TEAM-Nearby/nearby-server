# 동행 신고 API 연동

관련 이슈: [#242](https://github.com/TEAM-Nearby/nearby-server/issues/242).

## 신고 가능 범위

같은 매칭의 참여자는 일정 미확정, 만남 인증 전·중, 동행 중, 개인 동행 완료 후, 전체 동행 완료 후, 후기 작성 전·후에 상대방을 신고할 수 있다. 신고자와 상대방의 체크인 및 후기 작성 여부는 신고 자격에 영향을 주지 않는다.

취소된 매칭 또는 만남은 기존 정책대로 신고를 거절한다. 자기 신고와 같은 매칭의 비참여자에 대한 신고도 거절한다. 같은 매칭에서 같은 신고자가 같은 상대를 신고하는 것은 한 번만 허용한다. 일정 확정 전 신고와 확정 후 신고, 두 API를 번갈아 사용한 신고도 같은 중복 기준을 따른다.

## 요청과 응답

신규 API는 `POST /api/companion-matches/{matchId}/reports`다. 인증 토큰의 사용자를 신고자로 사용하며, 요청 본문은 기존 만남 기반 API와 같다.

```json
{
  "reportedUserId": 11,
  "reasons": ["SAFETY_THREAT"],
  "detail": "오픈채팅에서 협박성 발언을 받았습니다."
}
```

신고 사유는 `BAD_ATTITUDE`, `NO_SHOW`, `SAFETY_THREAT`, `MONEY_REQUEST`, `ETC`를 사용한다. 빈 목록과 중복 사유는 거절하고, `ETC`를 선택하면 상세 내용이 필요하다.

기존 기능과 같이 호스트는 `reportedUserId`를 생략하고 `detail`에 해당 매칭 게스트의 정확한 닉네임을 입력할 수 있다. 일반 상세 설명을 전달할 때는 사용자 ID를 명시한다.

성공 시 HTTP 201과 공통 응답의 `CREATE_COMPANION_REPORT` 코드를 반환한다. 신규 API의 `data`는 아래 구조다.

```json
{
  "matchId": 10,
  "reportId": 123,
  "reportedAt": "2026-10-21T06:00:00"
}
```

기존 `POST /api/companion-meetings/{meetingId}/reports`도 계속 사용할 수 있고, `data`의 `meetingId`, `reportId`, `reportedAt` 구조를 유지한다. 기존 만남에서 매칭을 찾아 같은 신고 로직을 실행한다.

| HTTP 상태 | 코드 | 의미 |
| --- | --- | --- |
| 400 | `INVALID_COMPANION_REPORT_REQUEST`, `INVALID_COMPANION_REPORT_REASON` | 식별자, 신고 사유 또는 상세 내용이 유효하지 않다. |
| 400 | `COMPANION_REPORT_SELF_NOT_ALLOWED` | 본인을 신고할 수 없다. |
| 400 | `COMPANION_REPORT_ALREADY_CANCELED` | 취소된 매칭 또는 만남이다. |
| 403 | `FORBIDDEN_COMPANION_REPORT` | 신고자가 해당 매칭의 참여자가 아니다. |
| 404 | `COMPANION_MATCH_NOT_FOUND`, `COMPANION_MEETING_NOT_FOUND` | 요청한 매칭 또는 만남이 없다. 만남 오류는 기존 API에만 해당한다. |
| 404 | `COMPANION_REPORT_TARGET_NOT_FOUND` | 매칭에 신고 대상자가 없거나 닉네임이 일치하지 않는다. |
| 404 | `COMPANION_REPORT_CONTEXT_NOT_FOUND` | 신고 이메일에 필요한 모집글·프로필 정보가 없다. |
| 409 | `COMPANION_REPORT_ALREADY_EXISTS` | 같은 매칭에서 해당 상대를 이미 신고했다. |

공통 인증, 온보딩 완료 및 활성 동행 프로필 접근 검사는 유지한다. 기존 신고의 미인증·개인 완료·진행 중 상태 제한 오류 세 개는 더 이상 반환하지 않는다.

## 앱에서 사용할 식별자와 후속 확인

- 진행 중인 동행 목록의 `matchId`를 신규 신고 API에 사용한다. 일정 미확정의 `meetingId = null`은 그대로 유지한다.
- `GET /api/companion-matches/{matchId}/preview`의 호스트·참여자 정보를 신고 대상 선택에 활용할 수 있다. 목록의 호스트를 현재 사용자 구분 없이 신고 대상으로 고정하지 않는다.
- 후기 대상 조회는 체크인한 상대만 반환한다. 노쇼 신고 대상을 후기 대상 목록에서만 찾으면 누락된다.
- 후기 화면에서 이미 보유한 매칭 또는 만남 ID로는 완료 후에도 신고할 수 있다. 다만 현재 진행 중인 목록은 완료 동행을 제외하고, 만남 상세도 완료 상태를 거절한다. 화면을 떠났다가 재진입하는 동선에 완료 동행 조회가 필요한지는 앱 담당자와 별도로 확인해야 한다. 이번 변경에는 이력 조회 API 확장을 포함하지 않는다.

## 저장과 배포

`V30__associate_companion_report_with_match.sql`은 기존 신고의 만남에서 `match_id`를 채우고, 필수 외래 키 및 `(match_id, reporter_user_id, reported_user_id)` 유일 제약을 적용한다. `meeting_id`는 선택 값으로 바뀐다. 일정 미확정 신고에 저장된 null 만남 ID를 이후 일정 확정 시 소급 변경하지 않아도 매칭 기준으로 중복을 판단할 수 있다.

두 API는 같은 매칭 행의 쓰기 잠금을 사용하고, DB 유일 제약으로도 중복을 차단한다. 신고와 사유는 한 트랜잭션으로 저장하고 이메일은 커밋 이후 발송한다. 이메일 조회는 확정 일정이 없어도 모집글·프로필 정보로 처리하며, 시간이 없으면 ‘일정 미확정’으로 표시한다.

마이그레이션 이후 구버전 서버는 필수 `match_id`를 채우지 못하므로, 구버전 신고 쓰기와 신버전 스키마가 동시에 사용되지 않도록 서버 전환을 조정해야 한다. 로컬 검증은 H2의 PostgreSQL 호환 모드이며, 운영 PostgreSQL 적용과 실제 이메일 전송은 별도 배포 검증 대상이다.
