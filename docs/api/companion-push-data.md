# 동행 푸시 알림 data와 iOS 화면 이동

관련 이슈: [#247](https://github.com/TEAM-Nearby/nearby-server/issues/247).

## Payload

FCM 메시지는 기존 `notification.title`, `notification.body`와 함께 다음 `data`를 전달한다.
아래 예시는 동행 신청 수락 알림이다. **data의 모든 값은 문자열**이며 숫자 형태의 ID도 문자열로 전달한다.

```json
{
  "type": "COMPANION_APPLICATION_ACCEPTED",
  "applicationId": "123",
  "notificationId": "456",
  "targetType": "COMPANION_APPLICATION",
  "targetId": "123"
}
```

| 키 | 의미 |
| --- | --- |
| `type` | 원본 알림 생성 당시의 사건 종류. 아래 세 값 중 하나이다. |
| `applicationId` | 동행 신청 ID. 화면 조회 API에 사용한다. |
| `notificationId` | 서버의 알림 ID. 읽음 처리와 중복 수신 식별에 사용한다. FCM의 메시지 ID와 다르다. |
| `targetType` | 기존 대상 종류. 현재 동행 알림은 `COMPANION_APPLICATION`이다. |
| `targetId` | 기존 대상 ID. 현재 동행 알림은 `applicationId`와 같다. |

기존 `notificationId`, `targetType`, `targetId`를 그대로 유지하고 `type`, `applicationId`를 추가한다.
제목·본문, TTL, 재시도 및 토큰 비활성화 정책은 유지한다.

## 알림별 화면 조회

| `type` | 수신자 | 이동할 화면과 조회 API |
| --- | --- | --- |
| `COMPANION_APPLICATION_CREATED` | 모집글 작성자. | 받은 신청 확인·수락/거절 화면. `GET /api/companion-requests/{applicationId}/review`. |
| `COMPANION_APPLICATION_ACCEPTED` | 신청자. | 신청 결과 화면. `GET /api/users/me/companion-requests/{applicationId}/result`. |
| `COMPANION_APPLICATION_REJECTED` | 신청자. | 신청 결과 화면. `GET /api/users/me/companion-requests/{applicationId}/result`. |

각 API는 `Authorization: Bearer <accessToken>`이 필요하며 기존 사용자·동행 프로필·접근 권한 검증을 따른다.
푸시를 받았다는 이유로 조회 권한이 생기지 않는다.

`type`은 **알림이 생성된 사건**이다. 제목·본문이나 발송 당시 신청 상태에서 추론하지 않는다.
예를 들어 신청 알림이 발송되기 전에 신청이 수락돼도 해당 신청 알림의 타입은 `COMPANION_APPLICATION_CREATED`이다.
앱은 `type`으로 조회 API를 선택하고 응답에 담긴 최신 상태로 화면과 버튼을 구성한다.
조회 시 대상 없음·권한 없음·결과 확인 불가 등의 오류가 반환되면 푸시의 과거 상태를 그대로 표시하지 않고 오류에 맞게 안내한다.

## 읽음 처리와 중복 수신

사용자가 알림을 열어 내용을 확인하면 다음 API를 호출한다. 요청 본문은 필요하지 않다.

```http
PATCH /api/users/me/companion-requests/456/read
Authorization: Bearer <accessToken>
```

경로에는 `applicationId`가 아닌 **`notificationId`**를 사용한다.
성공 응답의 `data`에는 `notificationId`, `isRead`, `readAt`이 포함된다.
이미 읽은 알림을 다시 처리해도 최초 `readAt`을 유지하므로 재호출할 수 있다.
푸시 발송 자체나 상세 조회가 읽음 처리를 대신하지 않는다.

동일 알림을 여러 기기로 발송하거나 재시도해도 `notificationId`, `applicationId`, `type`은 같다.
앱은 현재 계정의 `notificationId`로 중복 수신·중복 화면 이동을 식별할 수 있다.
같은 신청의 생성 알림과 수락·거절 알림은 별개 사건이므로 `applicationId`만으로 중복 제거하지 않는다.

## 서버 처리와 이전 메시지 호환

발송 배치마다 중복을 제거한 알림 ID 목록으로 원본 알림을 한 번에 조회한다.
원본 알림이 없는 발송은 `NOTIFICATION_NOT_FOUND`로 영구 실패 처리하고 FCM에 전달하지 않는다.
같은 배치의 정상 발송은 계속 처리하며, 이 오류로 기기 토큰을 비활성화하지 않는다.
DB 조회 자체가 실패한 경우에는 예외를 전달하고 기존 lease 복구 절차가 미완료 발송을 다시 처리한다.
DB 스키마 변경은 없으며, 배포 후 아직 발송되지 않은 기존 작업도 원본 알림에서 새 키를 구성한다.

이미 발송된 메시지에는 새 키가 없을 수 있다. 앱은 알 수 없는 `type`이나 누락된 키를 처리할 기본 경로를 둔다.
기존 키만 있으면 `targetType=COMPANION_APPLICATION`일 때 `targetId`로 신청 ID를 알 수 있지만,
신청·수락·거절은 구분할 수 없으므로 제목·본문으로 추론하지 않고 알림 목록 등 기본 화면으로 이동한다.

## 검증 범위

서버 테스트는 세 타입별 정확한 data, 중복 없는 배치 조회, 다중 기기·재시도 시 값 유지와 원본 누락 처리를 검증한다.
Firebase 어댑터 테스트는 실제 SDK가 직렬화한 HTTP 요청을 mock 전송 계층에서 확인한다.
이는 운영 FCM 접수나 실제 기기 수신 성공을 뜻하지 않는다.

배포 후 iOS에서 앱 실행 중·백그라운드·종료 상태의 알림 클릭 이동, 로그인 전환,
중복 수신, 이미 처리된 신청의 최신 상태 표시와 읽음 처리를 실제 기기로 확인해야 한다.
