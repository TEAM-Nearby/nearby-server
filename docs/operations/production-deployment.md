# 운영 EC2 수동 배포

GitHub Actions의 `Deploy production` 워크플로에서 브랜치를 `main`으로 선택합니다.

- 기본값 `check_only=true`는 AWS OIDC 인증, 운영 EC2의 SSM 명령 실행, Java·Caddy·CloudWatch 및 SSM 런타임 설정 복호화 권한을 확인합니다. 앱 JAR를 변경하거나 서비스를 재시작하지 않습니다.
- 실제 배포는 `check_only=false`로 실행합니다. `main`의 테스트와 `:bootstrap:bootJar` 빌드가 성공해야 S3 업로드와 SSM 배포가 진행됩니다.
- 새 환경 `production`은 `main` 브랜치만 허용합니다. `develop`의 기존 `CI` 수동 배포 대상은 그대로 유지합니다.
- 운영 대상은 `i-0e345dd1422312f87`, 고정 IP는 `54.116.193.222`입니다.
- 현재 운영 주소는 `http://54.116.193.222`, 준비 상태 확인 경로는 `/readyz`입니다. 운영 도메인과 HTTPS가 준비되면 `PROD_SERVICE_URL`을 갱신합니다.
- 런타임 SSM 경로 `/nearby/prod/env`, DB·Redis 접근 권한 및 CloudWatch 로그 그룹은 기존 서버와 공유합니다.
- OIDC 역할 `nearby-github-actions-production-deploy`는 운영 EC2에만 SSM 명령을 실행하고, 배포 버킷의 `production/releases/*` 경로만 읽고 쓸 수 있습니다. AWS Access Key를 GitHub에 저장하지 않습니다.
- GitHub `Settings → Environments → production`의 `PROD_*` 변수에서 연결 설정을 관리합니다. 역할 ARN은 비밀 값이 아니므로 `PROD_AWS_ROLE_ARN` 변수로 저장합니다.
- 동시에 들어온 운영 배포는 순서대로 실행되며 실행 중인 배포를 자동 취소하지 않습니다.

2026-10-15 설정 시점의 `main`은 초기 프로젝트 상태입니다. 실제 서버 배포 전에는 검토된 서버 릴리스를 `main`에 반영해야 합니다. 워크플로는 `bootstrap/build.gradle`이 없는 상태의 실제 배포를 거부합니다.

```bash
gh workflow run deploy-production.yml --repo TEAM-Nearby/nearby-server --ref main -f check_only=true
# 서버 릴리스를 main에 반영한 뒤 실제 배포합니다.
gh workflow run deploy-production.yml --repo TEAM-Nearby/nearby-server --ref main -f check_only=false
```
