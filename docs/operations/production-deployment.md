# 운영 EC2 수동 배포

GitHub Actions의 `Deploy production` 워크플로에서 브랜치를 `main`으로 선택합니다.

- 기본값 `check_only=true`는 AWS OIDC 인증, 운영 EC2의 SSM 명령 실행, Java·Caddy·CloudWatch 및 SSM 런타임 설정 복호화 권한을 확인합니다. 앱 JAR를 변경하거나 서비스를 재시작하지 않습니다.
- 실제 배포는 `check_only=false`로 실행합니다. `main`의 테스트와 `:bootstrap:bootJar` 빌드가 성공해야 S3 업로드와 SSM 배포가 진행됩니다.
- 새 환경 `production`은 `main` 브랜치만 허용합니다. `develop`의 기존 `CI` 수동 배포 대상은 그대로 유지합니다.
- 운영 대상은 `i-0e345dd1422312f87`, 고정 IP는 `54.116.193.222`입니다.
- 현재 운영 주소는 `http://54.116.193.222`, 준비 상태 확인 경로는 `/readyz`입니다. 운영 도메인과 HTTPS가 준비되면 `PROD_SERVICE_URL`을 갱신합니다.
- 운영 런타임 SSM 경로는 `/nearby/production/env`이며, `PROD_SSM_ENV_PATH`가 이 경로를 가리킵니다. 기존 개발 서버의 `/nearby/prod/env`와 저장소 변수 `SSM_ENV_PATH`는 유지합니다.
- 운영 DB는 서울 리전 Supabase `prod_nearby` 프로젝트(`jfzzriwqwvievxlyiiro`)입니다. session pooler `aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres`와 `postgres.jfzzriwqwvievxlyiiro` 계정, JDBC `sslmode=require`를 사용합니다. URL·사용자명·비밀번호는 운영 SSM의 `SecureString`으로 관리하며 저장소에는 비밀번호를 기록하지 않습니다.
- 운영 EC2는 전용 IAM 인스턴스 프로파일 `nearby-production-ec2`를 사용합니다. Redis, 프로필 이미지 저장소, CloudWatch 로그 그룹 및 DB 외 외부 연동 설정은 기존 구성을 재사용합니다. DB 분리가 전체 서비스 의존성의 격리를 의미하지는 않습니다.
- OIDC 역할 `nearby-github-actions-production-deploy`는 운영 EC2에만 SSM 명령을 실행하고, 배포 버킷의 `production/releases/*` 경로만 읽고 쓸 수 있습니다. AWS Access Key를 GitHub에 저장하지 않습니다.
- GitHub `Settings → Environments → production`의 `PROD_*` 변수에서 연결 설정을 관리합니다. 역할 ARN은 비밀 값이 아니므로 `PROD_AWS_ROLE_ARN` 변수로 저장합니다.
- 동시에 들어온 운영 배포는 순서대로 실행되며 실행 중인 배포를 자동 취소하지 않습니다.

2026-10-15 설정 시점의 `main`은 초기 프로젝트 상태입니다. 실제 서버 배포 전에는 검토된 서버 릴리스를 `main`에 반영해야 합니다. 워크플로는 `bootstrap/build.gradle`이 없는 상태의 실제 배포를 거부합니다.

```bash
gh workflow run deploy-production.yml --repo TEAM-Nearby/nearby-server --ref main -f check_only=true
# 서버 릴리스를 main에 반영한 뒤 실제 배포합니다.
gh workflow run deploy-production.yml --repo TEAM-Nearby/nearby-server --ref main -f check_only=false
```

## 2026-10-16 운영 DB 연결 검증

- 새 운영 EC2에서 운영 DB에 TLS 1.3으로 접속했으며, 명시적 읽기 전용 트랜잭션에서 연결 대상과 스키마 상태를 조회했습니다.
- `public` 앱 테이블은 0개이고 `flyway_schema_history`는 없습니다. 첫 앱 배포 시 Flyway 마이그레이션과 Hibernate 스키마 검증 결과를 확인해야 합니다.
- 운영 SSM 설정 30개를 `/opt/nearby/app.env`에 `nearby:nearby`, 권한 `0600`으로 설치하고 systemd `EnvironmentFile`에 연결했습니다.
- 기존 개발 SSM 설정은 값과 버전을 변경하지 않았습니다. 이 검증은 앱 배포, 마이그레이션 실행, `/readyz` 정상 응답을 확인한 결과와 구분합니다.
- 변경된 운영 설정으로 [Actions 연결 점검](https://github.com/TEAM-Nearby/nearby-server/actions/runs/38771937969)을 실행해 성공했습니다. `check_only=true` 실행이므로 실제 앱 배포는 생략했습니다.
