# 개발 서버 켜기·끄기

GitHub Actions의 **Dev server control**을 `develop`에서 수동 실행합니다.
대상은 AWS 계정 `395931907754`, 서울 리전의 개발 EC2 `i-06da8518d0a416c98` 한 대입니다.
운영 EC2, DB, Valkey, 저장소는 제어하지 않습니다.

## 처음 준비할 때

인프라 저장소의 `dev-server-control` 스택으로 전용 IAM 역할과 SSM 문서를 먼저 설치합니다.
OIDC provider는 기존 것을 사용하며 새 AWS Access Key나 GitHub Secret은 필요하지 않습니다.
Workflow는 계정과 역할 ARN을 명시하고, GitHub environment 없이 develop 브랜치 OIDC subject를 사용합니다.

1. `status`를 실행해 대상 서버가 running인지 확인합니다.
2. 앱이 정상 실행 중일 때 `prepare`를 실행합니다.
3. `prepare` 성공 후부터 `stop`과 `start`를 사용합니다.

`prepare`는 앱·Caddy·CloudWatch Agent의 부팅 자동 시작을 활성화합니다.
현재 앱을 재시작하지 않고 JAR, 환경 파일, DB를 수정하지 않습니다.
앱이 설치되지 않았거나 정상 실행 중이 아니면 먼저 기존 개발 배포 절차로 복구합니다.
SSM Agent 3.3.2746.0 이상이 필요합니다.

## 작업 선택

| action | 동작 | 성공의 의미 |
| --- | --- | --- |
| `status` | EC2 상태, SSM 연결, 외부 앱 상태를 조회합니다. | 조회 성공입니다. 서버가 꺼져 있거나 앱이 비정상일 수 있습니다. |
| `prepare` | 실행 중인 앱과 영구 파일을 확인하고 자동 시작을 활성화합니다. | 자동 시작 enabled와 기존 앱 프로세스 유지가 확인됐습니다. |
| `start` | 필요하면 EC2를 시작하고 준비 완료를 기다립니다. | EC2 상태 검사, SSM, 자동 시작·서비스 활성 상태, 내부·외부 `/readyz`를 확인했습니다. |
| `stop` | 자동 시작 설정을 검증한 뒤 EC2를 중지합니다. | EC2가 stopped 상태입니다. |

`stop`은 한 줄의 2~100자 사유와 정확한 확인 문구 `stop-dev`가 필요합니다.
EC2 ID나 운영 환경을 선택하는 입력은 제공하지 않습니다.

## 배포와 함께 사용할 때

이 workflow는 기존 **Deploy dev**와 직접 실행하는 개발 DB migration의
`nearby-operations-dev` concurrency 그룹을 사용합니다. 실행 중인 작업을 취소하지 않고 기다립니다.
대기 실행은 새 요청으로 대체될 수 있으므로 반복 실행하지 않습니다.
운영 작업의 concurrency 그룹과 파일은 바꾸지 않습니다.

개발 배포가 자동으로 EC2를 시작하지는 않습니다. 서버가 꺼져 있으면 `start` 성공 후 배포합니다.
서버 시작은 기존에 배포된 JAR를 자동으로 실행하며 새 코드를 배포하지 않습니다.

## 실패·시간 초과

- 앱 준비 상태는 HTTP 200뿐 아니라 JSON `status=UP`까지 확인합니다.
- `start`가 실패해도 EC2는 켜져 있을 수 있습니다. 진단을 위해 자동으로 다시 끄지 않습니다.
- `stop` 전에 자동 시작 검증이 실패하면 EC2 중지 API를 호출하지 않습니다.
- 이미 켜진 서버를 시작해도 재시작하지 않으며, 이미 꺼진 서버를 중지해도 다시 요청하지 않습니다.
- 실행 취소나 시간 초과는 이미 접수된 EC2·SSM 작업의 취소를 보장하지 않습니다.
  먼저 `status`와 SSM command ID로 실제 상태를 확인합니다.
- GitHub 밖의 AWS 콘솔 조작은 workflow 동시 실행 제한의 적용 대상이 아닙니다.

각 실행은 Summary와 `dev-server-control-<run-id>-<attempt>` artifact의 `result.json`에
작업자·사유·commit·진행 단계·전후 상태·SSM command ID를 기록합니다. Artifact 보관 요청 기간은 90일입니다.
`preflight_passed`는 입력 검증까지만 통과했다는 뜻이며 AWS 인증·서버 제어 성공이 아닙니다.
실제 제어 성공은 `result=success`와 해당 action의 검증 결과로 판단합니다.

이번 구현은 자동 종료 예약이나 웹 관리 화면을 추가하지 않습니다.
EC2를 중지해도 EBS·유지하는 Elastic IP·공유 Valkey·운영 서버 비용은 계속 발생합니다.
