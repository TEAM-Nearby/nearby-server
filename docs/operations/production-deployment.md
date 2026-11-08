# 운영 EC2 수동 배포

실제 배포는 Deploy prod 워크플로에서 main과 새 MAJOR.MINOR.PATCH 버전을 선택합니다.
기존 deploy-production.yml은 Check production access라는 권한 점검 전용 워크플로로 유지합니다.
CI/CD 설정과 migration·복구 절차는 [CI/CD 운영 문서](cicd.md)를 따릅니다.

- 기존 production Environment와 PROD_* 변수, PROD_AWS_ROLE_ARN을 유지합니다.
- Flyway DB 자격 증명은 production Environment의 FLYWAY_DB_URL·FLYWAY_DB_USER·FLYWAY_DB_PASSWORD로 추가합니다.
- PROD_INTERNAL_HEALTHCHECK_URL은 루프백 전체 Actuator health, PROD_SERVICE_URL과 PROD_HEALTHCHECK_PATH는 외부 HTTPS 검사에 사용합니다.
- 새 배포는 검증된 JAR·선행 migration·프로필 이미지 저장/조회 점검·내부/외부 상태 확인 후 릴리즈를 게시합니다.
- 기존 production/releases/* 전용 S3 권한은 새 nearby/* 산출물 경로에도 맞춰야 합니다.
- 아래 기록은 과거 연결 점검 결과이며 현재 스키마 상태나 새 배포 성공의 근거가 아닙니다.

## 2026-10-16 운영 DB 연결 검증

- 새 운영 EC2에서 운영 DB에 TLS 1.3으로 접속했으며, 명시적 읽기 전용 트랜잭션에서 연결 대상과 스키마 상태를 조회했습니다.
- `public` 앱 테이블은 0개이고 `flyway_schema_history`는 없습니다. 첫 배포 전 별도 Flyway migration과 Hibernate 스키마 검증 결과를 확인해야 합니다.
- 운영 SSM 설정 30개를 `/opt/nearby/app.env`에 `nearby:nearby`, 권한 `0600`으로 설치하고 systemd `EnvironmentFile`에 연결했습니다.
- 기존 개발 SSM 설정은 값과 버전을 변경하지 않았습니다. 이 검증은 앱 배포, 마이그레이션 실행, `/readyz` 정상 응답을 확인한 결과와 구분합니다.
- 변경된 운영 설정으로 [Actions 연결 점검](https://github.com/TEAM-Nearby/nearby-server/actions/runs/38771937969)을 실행해 성공했습니다. `check_only=true` 실행이므로 실제 앱 배포는 생략했습니다.
