# Docker 이미지 실행 가이드

## Docker Compose 로컬 실행

로컬 개발 환경은 Docker Compose로 애플리케이션, PostgreSQL, Redis를 함께 실행한다. 기본 DB는 Compose 내부 PostgreSQL이다.

Compose 실행에는 로컬 전용 `.env`가 필수다. 예시 파일을 참고해 `.env`를 만든다.

```bash
cp .env.example .env
```

`.env.example`은 커밋해도 되지만 실제 Secret이나 실제 dev DB 비밀번호를 넣지 않는다. 아래 값은 로컬에서 직접 정해야 하는 placeholder다.

```text
POSTGRES_DB=nearby
POSTGRES_USER=<local-postgres-user>
POSTGRES_PASSWORD=<local-postgres-password>
```

앱 컨테이너는 Compose 내부 서비스명인 `postgres:5432`와 `redis:6379`를 사용한다. 실제 dev DB 비밀번호나 운영 Secret은 Git에 커밋하지 않는다.

Compose 환경을 실행한다.

```bash
docker compose --env-file .env -f docker/docker-compose.yml up --build
```

로그에 `Started NearbyApplication`이 출력되면 애플리케이션 컨테이너가 정상 기동한 것이다.

PostgreSQL 데이터는 `postgres-data` Docker volume에 저장된다. DB를 초기화해야 할 때는 컨테이너 중단 후 volume 삭제 여부를 별도로 판단한다.

실행 중인 Compose 환경을 중단하고 컨테이너를 제거한다.

```bash
docker compose --env-file .env -f docker/docker-compose.yml down
```

## 이미지 빌드

```bash
docker build -t nearby-server:local .
```

이미지는 루트 `Dockerfile`을 사용해 빌드한다. 빌드 단계에서는 `./gradlew :app:bootJar`를 실행하고, 런타임 단계에는 생성된 jar만 포함한다.

## 컨테이너 실행 포트

애플리케이션 컨테이너는 `8080` 포트를 사용한다.

```bash
docker run --rm -p 8080:8080 nearby-server:local
```

## 필수 환경 변수

현재 애플리케이션은 JPA 자동 설정을 사용하므로 PostgreSQL 연결 정보가 없으면 Spring Boot 기동에 실패한다. Compose 실행 시 앱의 PostgreSQL과 Redis 주소는 내부 서비스명으로 자동 주입된다.

| 환경 변수 | 설명 | 예시 |
| --- | --- | --- |
| `POSTGRES_DB` | 로컬 PostgreSQL DB 이름 | `nearby` |
| `POSTGRES_USER` | 로컬 PostgreSQL 사용자 이름 | `<local-postgres-user>` |
| `POSTGRES_PASSWORD` | 로컬 PostgreSQL 비밀번호 | `<local-postgres-password>` |

Redis 의존성도 포함되어 있으므로 로컬 Compose 구성에서는 아래 값도 함께 정리한다.

| 환경 변수 | 설명 | 예시 |
| --- | --- | --- |
| `SPRING_DATA_REDIS_HOST` | Compose에서 자동 주입되는 Redis 호스트 | `redis` |
| `SPRING_DATA_REDIS_PORT` | Compose에서 자동 주입되는 Redis 포트 | `6379` |

Compose 내부 PostgreSQL은 호스트의 `5432` 포트로 노출된다.

dev Supabase session pooler에 연결해야 할 때는 Compose 기본 구성이 아니라 별도 실행 방식으로 DataSource 값을 주입한다. 실제 dev DB 비밀번호와 운영 Secret은 Dockerfile, `docker-compose.yml`, `.env.example`, 문서에 직접 작성하지 않는다.

## 로컬 컨테이너 기동 검증 예시

Compose 없이 #10 이미지만 검증할 때는 임시 PostgreSQL 컨테이너를 먼저 실행한 뒤 애플리케이션 컨테이너에 DataSource 환경 변수를 주입한다.

아래 예시의 `LOCAL_POSTGRES_PASSWORD`는 로컬 셸에서만 설정하는 임시 값이며 Git에 커밋하지 않는다.

```bash
docker run --rm --name nearby-postgres-local \
  -e POSTGRES_DB=nearby \
  -e POSTGRES_USER=nearby \
  -e POSTGRES_PASSWORD="$LOCAL_POSTGRES_PASSWORD" \
  -p 55432:5432 \
  postgres:16-alpine
```

다른 터미널에서 애플리케이션 컨테이너를 실행한다.

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:55432/nearby \
  -e SPRING_DATASOURCE_USERNAME=nearby \
  -e SPRING_DATASOURCE_PASSWORD="$LOCAL_POSTGRES_PASSWORD" \
  nearby-server:local
```

로그에 `Started NearbyApplication`이 출력되면 이미지와 환경 변수 주입이 정상 동작한 것이다.
