# Nearby CI/CD 운영

## 실행 구조

Java 21, Spring Boot 3.5, Gradle 멀티모듈과 EC2·S3·SSM·systemd·GitHub OIDC를 유지한다.
Lambda, Docker 기반 앱 배포, API Gateway, CloudFront는 추가하지 않는다.
실행 JAR는 bootstrap 모듈 하나이며 모듈 의존 방향은 바뀌지 않는다.

| 파일 | 역할 |
| --- | --- |
| .github/workflows/ci.yml | develop/main/release/** 대상 PR·push 검증. 배포 권한이 없다. |
| .github/workflows/verify.yml | CI와 배포의 공통 검증 및 JAR artifact 생성. |
| .github/workflows/deploy-dev.yml | develop에서 수동 개발 배포. |
| .github/workflows/deploy-prod.yml | main에서 수동 운영 배포 후 태그·Release 게시. |
| .github/workflows/deploy.yml | 사전 검사 → 검증·JAR → migration → S3·SSM 배포. |
| .github/workflows/flyway-migration.yml | 재사용 workflow_call 및 단독 workflow_dispatch. |
| .github/workflows/database-backup.yml | 기본 브랜치에서 KST 05:00 예약·수동 백업. |
| .github/release.yml | PR 라벨별 Release 노트 분류 및 skip-release-notes 제외. |
| scripts/deploy/pipeline.py | 브랜치·SHA·설정·버전 검증, JAR 포장, 릴리즈 게시. |
| scripts/deploy/deploy.py | S3 업로드, SSM 관찰, 외부 HTTP 검증, metadata 저장. |
| scripts/deploy/ec2.py | SSM에서 실행하는 EC2 배포 프로그램. |
| scripts/backup/backup.py | 분리 덤프, 압축·암호화, 검증된 최신 백업 게시. |

배포 전체에서 실행 시 확정된 github.sha를 checkout한다. 동일 SHA의 필수 검증을 다시 실행하므로
별도 CI 상태에만 의존하지 않는다. 검증한 JAR를 artifact로 전달하고 배포 job에서는 재빌드하지 않는다.
JAR 포장 단계는 변경·미추적 파일이 있는 작업 트리를 거부한다. 로컬 수정 JAR를 HEAD의 산출물로 표시하지 않는다.
테스트·빌드를 DB 변경 전에 끝내어 빌드 실패 시 DB만 먼저 변경되는 상황을 줄인다.

CI는 이전 실행을 취소할 수 있다. 배포는 dev/prod별 nearby-operations 잠금으로 migration부터
릴리즈까지 직렬화하며 진행 중 실행을 취소하지 않는다. 단독 migration도 같은 operations 잠금을 사용한다.
배포 내부 migration은 부모가 이미 operations 잠금을 보유하므로 별도 migration 잠금을 사용한다.
새 호출자는 deployment_lock_held=true를 전달하면서 부모 잠금을 생략하면 안 된다.
GitHub 기본 concurrency는 무제한 FIFO 대기열이 아니며 대기 실행은 새 요청으로 대체될 수 있다.

## CI 검증

    ./gradlew spotlessCheck checkstyleMain checkstyleTest checkstyleMigration test :bootstrap:bootJar --no-daemon
    python3 -m unittest discover -s scripts/tests -v

- Spotless 8.8.0은 끝 공백·파일 끝 개행을 검사한다. 기존 들여쓰기·import 순서를 일괄 변경하지 않는다.
  build.gradle에 고정한 도입 기준 커밋 이후 변경된 Java 파일부터 적용하므로 전체 Git 이력이 필요하다.
- Checkstyle 12.3.1은 equals/hashCode 짝, 빈 문장, JDK 내부 패키지 import를 검사한다.
- 기존 테스트와 Spring Modulith 경계 검증을 실행한다.
- CI 임시 PostgreSQL 17.6에서 모든 SQL을 적용하고 재실행한다. 운영 DB 버전을 확인했다는 뜻은 아니다.
  실제 Supabase 버전 확인 후 CI 이미지도 맞춘다.
- actionlint 1.7.7은 공식 체크섬으로 검증한 뒤 실행한다.
- Python 테스트는 외부 자원에 연결하지 않고 배포 차단, SSM 실패 단계, 백업 부분 실패를 검증한다.

## DB migration

bootstrap의 별도 migration 소스 세트가 앱 runtimeClasspath를 사용한다. 현재 Flyway는 11.7.2다.
별도 CLI 버전 대신 앱과 같은 의존성을 사용하며 migration 실행 클래스는 일반 bootJar에 포함하지 않는다.

    # FLYWAY_URL, FLYWAY_USER, FLYWAY_PASSWORD를 안전하게 주입한 뒤 실행한다.
    ./gradlew :bootstrap:migrateDatabase --no-daemon

info → validate → migrate 순서로 실행한다. SQL은 아래 두 경로를 모두 사용한다.

- adapter-out/persistence/src/main/resources/db/migration.
- adapter-out/persistence/src/main/resources/db/migration-postgresql.

public 스키마와 flyway_schema_history를 사용한다. pending은 허용하지만 코드에 없는 미래 버전,
체크섬 변경, 누락 migration은 실패한다. 자동 baseline·clean·out-of-order 적용은 하지 않는다.
기존 DB에 히스토리가 없다면 스키마 대조·백업 후 기준 버전을 정하는 별도 초기 작업이 필요하다.

EC2 환경 파일 작성 시 SPRING_FLYWAY_ENABLED=false와 SPRING_JPA_HIBERNATE_DDL_AUTO=validate를
강제한다. 로컬·테스트의 기존 기동 migration은 유지한다. systemd ExecStart에서 이 값을
명령행으로 다시 덮어쓰면 배포를 거부한다. 런타임과 migration 계정은 분리할 수 있지만
반드시 같은 DB·public 스키마를 가리켜야 한다.

새 SQL은 현재 실행 중인 이전 앱과 호환되어야 한다. 삭제·이름 변경은 추가, 앱 전환,
데이터 정리, 제거를 여러 배포로 나눈다. 적용된 SQL은 수정하지 않고 새 migration으로 보정한다.
DB는 자동 rollback하지 않는다. GitHub runner의 DB 네트워크·SSL 연결을 준비하고 Supabase
direct 연결이 불가능하면 검증된 session pooler를 사용한다.

## 최신 develop 운영 설정 호환

기존 production Environment와 PROD_* Variables, PROD_AWS_ROLE_ARN을 그대로 사용한다.
논리 입력 prod는 GitHub Environment production으로 연결하며, dev는 dev Environment를 사용한다.
아래 표의 공통 Variable 이름은 운영에서 PROD_ 접두어를 붙여 읽는다.
운영 Role은 Secret AWS_ROLE_TO_ASSUME 대신 기존 Variable PROD_AWS_ROLE_ARN을 사용한다.
Flyway Secret 이름은 공통이며 production Environment에 저장한다.
prod-backup의 AWS_REGION·AWS_ACCOUNT_ID는 접두어 없이 별도 설정한다.
기존 deploy-production.yml은 권한 점검 전용으로 전환하여 선행 migration을 우회하는 배포를 제거한다.
기존 프로필 이미지 저장·CDN 조회 점검은 새 운영 배포에서도 JAR 교체 전에 실행한다.
기존 버킷의 production/releases/* 전용 IAM 정책은 새 nearby/* 경로에도 맞춰야 한다.

## GitHub 설정

Environment 이름만 나누어서는 인프라가 격리되지 않는다. 개발·운영의 EC2 서비스, DB,
Parameter Store 경로를 실제로 분리한다. 아래 값은 실제 리소스를 확인해 입력한다.
Environment로 이전하면서 같은 이름의 Repository 값이 잘못 fallback되지 않는지 확인한다.

| 이름 | 유형·범위 | 용도 |
| --- | --- | --- |
| AWS_REGION | Variable / dev, prod, prod-backup | AWS 리전. |
| AWS_ACCOUNT_ID | Variable / dev, prod, prod-backup | 12자리 계정 ID와 OIDC 계정 검증. |
| AWS_ROLE_TO_ASSUME | Secret / dev, prod | 기존 배포 OIDC Role ARN. |
| EC2_INSTANCE_ID | Variable / dev, prod | 배포 대상 관리형 인스턴스. |
| DEPLOY_S3_BUCKET | Variable / dev, prod | JAR 보관 버킷. |
| APP_DIR | Variable / dev, prod | 디스크에 존재하는 앱 디렉터리. |
| APP_JAR_PATH | Variable / dev, prod | systemd가 실행하는 APP_DIR 내부 JAR 절대 경로. |
| APP_SERVICE_NAME | Variable / dev, prod | 기존 systemd 서비스 이름. |
| SSM_ENV_PATH | Variable / dev, prod | 런타임 Parameter Store 경로. |
| INTERNAL_HEALTHCHECK_URL | Variable / dev, prod | 루프백 HTTP /actuator/health. monitoring 사용 시 관리 포트 9091. |
| SERVICE_URL | Variable / dev, prod | 외부 HTTPS 기본 URL. |
| HEALTHCHECK_PATH | Variable / dev, prod | 외부 JSON 상태 검사 경로. monitoring 사용 시 /readyz. |
| FLYWAY_DB_URL | Secret / dev, prod | PostgreSQL JDBC URL. |
| FLYWAY_DB_USER | Secret / dev, prod | migration 권한을 가진 사용자. |
| FLYWAY_DB_PASSWORD | Secret / dev, prod | migration 계정 암호. |
| BACKUP_ENABLED | Variable / Repository | 외부 설정 완료 후 문자열 true로 활성화. 기본 비활성. |
| PROD_DB_BACKUP_ROLE_ARN | Variable / prod-backup | 백업 전용 OIDC Role ARN. |
| PROD_BACKUP_S3_BUCKET | Variable / prod-backup | 암호화된 백업 전용 버킷. |
| PROD_SUPABASE_DB_URL | Secret / prod-backup | Supabase CLI용 PostgreSQL URI. JDBC URL이 아니다. |
| BACKUP_ENCRYPTION_PASSPHRASE | Secret / prod-backup | GPG 암호. 복구 가능한 별도 보관소에도 보관한다. |

기존 APP_PORT, DOMAIN_NAME은 새 워크플로에서 사용하지 않는다.
내부·외부 포트가 다를 수 있으므로 상태 검사 URL을 구분한다.
prod-backup Environment는 기본 브랜치 develop의 예약 실행과 prod의 main-only 규칙을 함께 유지하기 위한 것이다.

보호 규칙과 권한은 다음과 같이 준비한다.

- develop/main의 PR 필수 검사에 CI의 Backend checks를 연결한다. 실제 최초 실행에 표시되는 check 이름을 사용한다.
- dev는 develop만, production은 main만 허용한다. prod 승인은 팀 정책에 맞게 설정한다.
- prod-backup은 현재 기본 브랜치만 허용한다. 예약 작업이 매번 수동 승인을 기다리지 않도록 별도 정책을 정한다.
- 워크플로·배포 스크립트 변경을 리뷰하고 be-v* 태그의 이동·삭제를 막되 봇의 최초 생성은 허용한다.
- Release job만 contents:write, AWS 접근 job만 id-token:write를 사용한다.

## AWS·EC2 준비

OIDC를 유지한다. Environment 연결 후 IAM 신뢰 조건 sub를 다음 형식으로 맞추고
aud는 sts.amazonaws.com으로 설정한다. 각 Role은 해당 Environment만 신뢰한다.

    repo:TEAM-Nearby/nearby-server:environment:dev
    repo:TEAM-Nearby/nearby-server:environment:production
    repo:TEAM-Nearby/nearby-server:environment:prod-backup

| 주체 | 필요한 권한과 준비 |
| --- | --- |
| 배포 OIDC Role | 배포 버킷 nearby/*의 PutObject/GetObject, 지정 EC2와 AWS-RunShellScript의 SendCommand, GetCommandInvocation. |
| EC2 instance profile | SSM 관리형 인스턴스 권한, JAR GetObject, 해당 경로의 GetParametersByPath, SecureString용 KMS Decrypt. |
| 백업 OIDC Role | 백업 버킷 nearby-db/*의 PutObject/GetObject. head-object도 GetObject 권한을 사용한다. |
| KMS 사용 버킷 | 해당 키의 Encrypt/Decrypt/GenerateDataKey 등 실제 버킷 정책에 필요한 권한. |

S3 Block Public Access, TLS, 보존·암호화 정책은 별도 리소스 설정이다.
JAR는 nearby/{SHA}/{run-id}-{attempt}/app.jar에 저장하고 If-None-Match로 덮어쓰기를 거부한다.
동일 SHA 재빌드가 바이트 단위로 같다고 가정하지 않고 실제 SHA256을 기록한다.

EC2에는 Java 21, Python 3.9 이상, AWS CLI v2, systemd, findmnt, SSM Agent,
nearby 사용자와 기존 서비스가 필요하다. APP_DIR는 디스크 파일시스템이어야 하며 심볼릭 링크를 사용하지 않는다.
SSM은 root로 실행하고 서비스는 APP_JAR_PATH를 java -jar의 인자로 그대로 사용한다.
부모 디렉터리를 포함해 nearby 사용자가 JAR를 읽을 수 있어야 한다.

Parameter Store의 DB·Redis·JWT·외부 서비스 설정을 유지한다.
SPRING_PROFILES_ACTIVE에는 supabase를 포함한다. monitoring을 사용하면 내부 Actuator는
127.0.0.1:9091, 외부 readiness는 앱 포트의 /readyz다.
내부 전체 health에서 DB·Redis도 확인한다. HTTP 200뿐 아니라 JSON status=UP을 검사한다.
Parameter의 마지막 경로 요소를 환경변수 이름으로 사용하고 중복·개행 값은 거부한다.
여러 줄 인증서는 안전한 파일 주입 방식을 별도로 구성한다.

## 첫 배포

1. GitHub Environments, OIDC 신뢰·권한, EC2, S3, DB, Parameter Store를 준비한다.
2. DB 히스토리·체크섬과 이전 앱의 SQL 호환성을 확인한다.
3. develop에 워크플로를 반영하고 CI를 통과시킨 후 Deploy dev를 수동 실행한다.
4. migration·내부 상태·외부 HTTP·metadata를 확인한다. 실제 API·모바일 동작도 별도로 확인한다.
5. 검증한 변경을 main에 반영하고 CI를 통과시킨다. 실제 운영 실행 지시 후 새 버전으로 Deploy prod를 실행한다.
6. 운영 배포 성공 metadata 저장 뒤에만 annotated tag와 GitHub Release를 게시한다.

단일 systemd 서비스를 정지·교체·기동하므로 중단 시간이 있다. 무중단 전환은 제공하지 않는다.
서버 파일 잠금도 사용하며 Actions 취소가 SSM 명령의 즉시 종료를 뜻하지는 않는다.
SSM 실행 제한 600초, 전달 제한 60초, runner 관찰 제한 720초다.
내부 검사는 120초, 외부 검사는 90초를 기준으로 개별 HTTP 요청 timeout도 설정한다.

## 실패와 수동 복구

metadata에는 SHA, JAR 체크섬, S3 위치·객체 버전, EC2 ID, SSM 명령 ID, 릴리즈 버전,
이전 SHA·체크섬, 서버 복구 경로, 단계·결과를 남긴다. 비밀값은 남기지 않는다.

| 실패 시점 | 대응 |
| --- | --- |
| 사전 검사·CI | DB·앱 변경 전이다. 입력이나 코드를 수정한다. |
| migration | 앱 배포를 실행하지 않는다. 실제 DB 상태를 확인하고 forward migration으로 보정한다. |
| 업로드·원격 before_switch | DB는 변경됐을 수 있지만 JAR 교체는 시작하지 않았다. 이전 앱 호환성을 확인한다. |
| switch_started 이후 | 서비스 정지 또는 JAR 교체 가능성이 있다. 이전 앱 실행을 단정하지 않는다. |
| remote_state_unknown·unknown_timeout | SSM 명령 ID, 서버 실행·파일 잠금 상태부터 확인하고 바로 재배포하지 않는다. |
| 외부 HTTP | 내부 검증은 통과했을 수 있다. 프록시·DNS·외부 연결을 앱과 구분한다. |
| Release 게시 | 앱 배포 성공과 별개다. 전체 재배포나 태그 이동·삭제를 하지 않는다. |

앱 복구는 metadata의 recovery_directory 안에 저장한 previous.jar, previous.env,
previous-dropin.conf를 사용한다. 경로는 실행마다 분리되고 비밀값은 EC2 안에만 저장한다.

1. SSM 명령 종료, 현재·이전 SHA, DB 호환성을 확인한다.
2. 승인된 운영 작업으로 서비스를 정지하고 previous.jar·previous.env를 원래 위치에 복원한다.
3. previous-dropin.conf가 있으면 90-nearby-deployment.conf를 복원한다.
   없으면 이번 배포가 만든 drop-in만 제거한다.
4. daemon-reload 후 기동하여 내부·외부 health와 실제 API를 확인한다.
5. deployment.json도 previous-deployment.json으로 복원하거나 최초 배포였음을 기록한다.

이 절차는 자동 실행하지 않는다. 복구 환경도 SPRING_FLYWAY_ENABLED=false와 ddl-auto=validate를 유지한다.
이전 앱과 DB가 호환되지 않으면 이전 JAR 대신 수정 배포를 선택한다.
서버 releases·복구 디렉터리는 자동 삭제하지 않으므로 디스크 용량과 보존 기간을 관리한다.

Release 단계만 실패했다면 실패한 job만 재실행할 수 있다. 같은 run의 저장된 metadata를 재사용하며
기존 annotated tag가 같은 SHA일 때만 이어서 게시한다. 다른 SHA나 lightweight tag는 거부한다.
전체 배포를 새로 실행하면 기존 버전 중복 검사에서 중단된다.
metadata artifact 보존 기간은 90일이며 장기 복구 기록은 별도 보관한다.

## 백업과 복원

BACKUP_ENABLED=true 전에는 예약·수동 백업 모두 실행되지 않는다. 외부 설정 완료 후 활성화하고
기본 브랜치에서 첫 수동 백업을 검증한다. UTC 20:00은 KST 다음 날 05:00이며 실제 시작은 지연될 수 있다.

Supabase CLI 2.58.5를 고정하고 Docker 사용 가능한 runner에서 역할·스키마·데이터를 분리한다.
빈 파일을 검사하고 gzip·GPG AES256으로 암호화한 뒤 평문을 삭제한다. 실패해도 임시 파일을 정리한다.
로그에는 DB URL·암호·SQL을 출력하지 않는다.

- daily는 한국 날짜·실행 ID 아래 암호문 3개와 SHA256·크기 manifest를 저장한다.
- 한국 시간 1일에는 monthly에 별도 암호문 사본과 manifest를 저장한다.
- latest/manifest.json은 모든 업로드 검증이 끝난 daily 세트를 가리킨다.
  파일별 최신 사본을 순차 덮어쓰지 않아 부분 실패 시 서로 다른 세트가 섞이지 않는다.
- S3 Lifecycle로 보존을 정한다. latest가 가리키는 daily 세트가 만료되지 않도록
  백업 실패 알림·보존 기간을 함께 정한다.
- 별도 DB에서 다운로드 → SHA256 확인 → 복호화 → gzip 해제 → 역할·스키마·데이터 복원 →
  핵심 테이블·제약·앱 동작을 검증한다. 업로드 성공은 복원 성공이 아니다.
- Supabase 관리 스키마, 확장, 역할 암호, Storage 객체, 외부 S3 파일은 기능별로 복구 범위를 확인한다.
- 개별 덤프는 하나의 원자적 전체 스냅샷을 보장하지 않는다. 백업 시간대 DDL을 피하고
  정밀한 시점 복구가 필요하면 플랫폼 PITR을 별도로 구성한다.

실패 알림, Lifecycle, 암호의 별도 보관, 정기 복원 연습은 외부 운영 설정이다.
Environment를 추가해도 AWS Role·버킷이 자동으로 만들어지지는 않는다.

## 원본과의 차이

척척학사의 자동 CI·수동 환경별 배포·선행 Flyway·배포 후 릴리즈·독립 백업 원칙을 유지한다.
Lambda는 기존 EC2 JAR 배포로, Access Key는 기존 OIDC로 대체한다.
dev 대신 develop, 멀티모듈의 두 SQL 경로, 앱과 동일한 Flyway 의존성을 사용한다.
정적 웹 소스가 없어 웹 배포는 추가하지 않았다.

- [GitHub 재사용 워크플로](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows).
- [GitHub OIDC와 AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws).
- [AWS SSM 명령 상태](https://docs.aws.amazon.com/systems-manager/latest/userguide/monitor-commands.html).
- [Supabase 백업](https://supabase.com/docs/guides/deployment/ci/backups).
- [척척학사 운영 배포](https://github.com/cchaksa/cchaksa-backend/blob/dev/.github/workflows/deploy-prod-lambda.yml).

## 구현 시 검증 기록

2026-11-08 로컬 작업 트리를 기준으로 다음을 확인했다.

- 포맷·Checkstyle·전체 테스트·bootJar 빌드 성공. 현재 모듈의 Java 테스트 결과 944개, 실패·오류·건너뜀 0개.
  변경 없는 모듈은 Gradle의 기존 통과 결과를 재사용했다.
- Python 배포·백업 테스트 18개 성공.
- actionlint 문법 검사와 diff 공백 검사 성공. 로컬 actionlint 실행에는 별도 shellcheck·pyflakes를 사용하지 않았다.
- 임시 PostgreSQL 14.18에 최신 develop 기반 SQL 31개 적용, 두 번째 실행의 변경 없음,
  체크섬 불일치와 코드에 없는 미래 버전 차단을 확인했다. 테스트 서버는 종료했다.
- 실행 JAR의 Main-Class·Start-Class 및 migration 실행 클래스 미포함을 확인했다.

최신 develop 기반 격리 작업 트리의 결과이며 기존 사용자 변경은 포함하지 않았다. 원격 CI와 CI의 PostgreSQL 17.6 컨테이너는
실행하지 않았다. 실제 AWS OIDC·SSM·systemd·외부 HTTP, Supabase 덤프·GPG·S3 업로드 및 복원은
외부 설정 후 별도 검증해야 한다. 운영 migration·배포·태그·Release를 실행하지 않았다.
