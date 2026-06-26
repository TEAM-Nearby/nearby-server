# Docker 이미지 실행 가이드

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

현재 애플리케이션은 JPA 자동 설정을 사용하므로 PostgreSQL 연결 정보가 없으면 Spring Boot 기동에 실패한다.

| 환경 변수 | 설명 | 예시 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://host.docker.internal:55432/nearby` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL 사용자 이름 | `nearby` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL 비밀번호 | `nearby` |

Redis 의존성도 포함되어 있으므로 로컬 Compose 구성에서는 아래 값도 함께 정리한다.

| 환경 변수 | 설명 | 예시 |
| --- | --- | --- |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `redis` |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | `6379` |

## 로컬 컨테이너 기동 검증 예시

Compose 없이 #10 이미지만 검증할 때는 임시 PostgreSQL 컨테이너를 먼저 실행한 뒤 애플리케이션 컨테이너에 DataSource 환경 변수를 주입한다.

```bash
docker run --rm --name nearby-postgres-local \
  -e POSTGRES_DB=nearby \
  -e POSTGRES_USER=nearby \
  -e POSTGRES_PASSWORD=nearby \
  -p 55432:5432 \
  postgres:16-alpine
```

다른 터미널에서 애플리케이션 컨테이너를 실행한다.

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:55432/nearby \
  -e SPRING_DATASOURCE_USERNAME=nearby \
  -e SPRING_DATASOURCE_PASSWORD=nearby \
  nearby-server:local
```

로그에 `Started NearbyApplication`이 출력되면 이미지와 환경 변수 주입이 정상 동작한 것이다.
