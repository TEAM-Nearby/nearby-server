# ADR-02 database settings

## 상태

Accepted.

## 작성일

2026-07-02.

## 목적

Nearby 서버가 Supabase PostgreSQL을 런타임 데이터베이스로 사용할 수 있도록 연결 방식, 스키마 관리 방식, 운영 검증 방식을 정리합니다.

이 문서는 백엔드 팀원이 다음 내용을 빠르게 이해하는 것을 목표로 합니다.

1. 애플리케이션이 어떤 설정으로 Supabase PostgreSQL에 연결되는지.
2. 빈 Supabase DB에 테이블이 어떤 순서와 방식으로 생성되는지.
3. Hibernate가 스키마를 자동 변경하지 않고 검증만 하도록 둔 이유.
4. `/actuator/health`의 `UP`이 무엇을 의미하는지.
5. 로컬, 배포, CI에서 어떤 환경변수가 필요한지.

## 결정 요약

| 항목 | 결정 |
| --- | --- |
| DB 제품 | Supabase PostgreSQL |
| 연결 방식 | 기본은 Supabase session pooler |
| 설정 주입 | 환경변수 기반 `spring.datasource.*` |
| connection pool | Spring Boot 기본 HikariCP 사용, 기본 maximum pool size는 `5` |
| 스키마 생성과 변경 | Flyway migration SQL로 관리 |
| Hibernate DDL 정책 | `ddl-auto=validate` |
| PostgreSQL JDBC driver | 기존 runtime dependency 유지 |
| Supabase SDK | 사용하지 않음 |
| Redis health | Supabase 프로필에서는 비활성화 |

## 배경

Nearby는 Java 21, Spring Boot 3.5, Spring Modulith 기반 단일 애플리케이션입니다. 현재 모듈 구조상 실행 조립은 `bootstrap`이 맡고, JPA Entity와 Repository 같은 영속성 구현은 `adapter-out:persistence`가 맡습니다.

따라서 DB 연결 설정은 `bootstrap`에 두고, DB 스키마 migration은 persistence adapter의 resource에 둡니다. 이렇게 하면 실행 환경 설정과 영속성 구현 책임이 섞이지 않습니다.

## 관련 파일

| 파일 | 역할 |
| --- | --- |
| `bootstrap/src/main/resources/application-supabase.yaml` | Supabase 프로필에서 사용할 DataSource, Flyway, health 설정을 정의합니다. |
| `.env.example` | 팀원이 준비해야 할 환경변수 형식을 예시로 제공합니다. 실제 secret은 포함하지 않습니다. |
| `bootstrap/build.gradle` | PostgreSQL JDBC driver와 Flyway PostgreSQL runtime dependency를 제공합니다. |
| `adapter-out/persistence/src/main/resources/db/migration/V1__create_initial_schema.sql` | 초기 PostgreSQL 테이블, 외래키, unique constraint를 생성합니다. |
| `adapter-out/persistence/src/main/java/.../*Entity.java` | JPA Entity와 PostgreSQL 컬럼 타입의 대응을 정의합니다. |
| `bootstrap/src/main/resources/application.yaml` | 공통 Spring 설정을 정의합니다. 현재는 Servlet Filter proxy 초기화 문제를 피하기 위해 JDK proxy 방식을 사용합니다. |

## 런타임 연결 흐름

Supabase DB 연결은 애플리케이션 기동 시 다음 순서로 진행됩니다.

1. 실행 환경에서 `SPRING_PROFILES_ACTIVE=supabase`를 주입합니다.
2. Spring Boot가 기본 `application.yaml`과 추가 `application-supabase.yaml`을 함께 읽습니다.
3. `application-supabase.yaml`의 placeholder가 실제 환경변수 값으로 치환됩니다.
4. Spring Boot auto-configuration이 `spring.datasource.*` 값으로 Hikari DataSource를 만듭니다.
5. Flyway가 같은 DataSource로 DB에 접속해 migration 적용 여부를 확인합니다.
6. 적용되지 않은 migration이 있으면 버전 순서대로 실행하고 `flyway_schema_history`에 기록합니다.
7. Hibernate가 JPA Entity와 실제 DB 스키마를 `ddl-auto=validate`로 비교합니다.
8. 검증이 통과하면 JPA Repository와 애플리케이션이 정상 기동합니다.
9. Actuator health endpoint가 같은 DataSource로 DB 연결 상태를 확인합니다.

## 환경변수

Supabase 프로필에서 필요한 값은 다음과 같습니다.

```bash
SPRING_PROFILES_ACTIVE=supabase
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-<region>.pooler.supabase.com:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.<project-ref>
SPRING_DATASOURCE_PASSWORD=<database-password>
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
```

로컬 개발에서는 `.env`에 실제 값을 둘 수 있지만, `.env`는 git에 커밋하지 않습니다. 저장소에는 `.env.example`만 커밋합니다.

배포 환경에서는 같은 값을 배포 플랫폼의 secret manager, environment variable, CI/CD secret에 넣습니다.

## Supabase 연결 방식

Supabase PostgreSQL은 direct connection과 pooler connection을 제공합니다.

이번 작업에서는 실제 검증 결과 session pooler를 사용합니다.

```bash
jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres?sslmode=require
```

Direct connection 형식은 다음과 같습니다.

```bash
jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require
```

현재 작업 환경에서는 direct host가 DNS 해석되지 않았습니다. 반면 session pooler는 실제 접속, Flyway migration, Hibernate validation, actuator DB health까지 통과했습니다.

Transaction pooler는 기본안에서 제외했습니다. 서버리스처럼 짧은 연결이 매우 많은 환경에서 검토할 수 있지만, JDBC prepared statement 설정까지 함께 조정해야 해서 현재의 지속 실행 Spring Boot 서버에는 session pooler가 단순하고 적절합니다.

## Flyway 스키마 관리

Flyway는 DB 스키마 변경을 버전이 붙은 SQL 파일로 관리합니다.

초기 스키마는 다음 파일입니다.

```text
adapter-out/persistence/src/main/resources/db/migration/V1__create_initial_schema.sql
```

Flyway 파일명 규칙상 `V1`은 schema version이고, `create_initial_schema`는 설명입니다.

기동 시 Flyway는 다음 작업을 수행합니다.

1. `db/migration` 경로의 migration SQL을 읽습니다.
2. DB에 `flyway_schema_history` 테이블이 없으면 생성합니다.
3. `flyway_schema_history`에 없는 migration을 찾습니다.
4. 아직 적용되지 않은 SQL을 버전 순서대로 실행합니다.
5. 실행 성공 기록을 `flyway_schema_history`에 저장합니다.
6. 다음 기동부터는 이미 적용된 `V1`을 다시 실행하지 않습니다.

이 방식의 핵심은 DB 구조 변경 이력이 코드 리뷰 가능한 SQL로 남는다는 점입니다.

## Hibernate DDL 정책

Supabase 프로필에서는 Hibernate 설정을 다음처럼 둡니다.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

`validate`는 테이블을 만들거나 바꾸지 않습니다. Hibernate는 Entity와 DB 스키마가 맞는지만 검사합니다.

`update`를 사용하지 않은 이유는 운영 DB 스키마를 Hibernate가 예측하기 어려운 방식으로 변경할 수 있기 때문입니다. 운영 DB 변경은 Flyway SQL로 명시적으로 수행하고, Hibernate는 검증 역할만 맡습니다.

## PostgreSQL text 컬럼 정렬

초기 검증 중 `@Lob String` 필드가 PostgreSQL `text` 컬럼과 맞지 않는 문제가 있었습니다.

Hibernate는 일부 `@Lob String` 매핑에서 PostgreSQL 대형 객체 타입을 기대할 수 있고, 이 경우 Flyway가 만든 `text` 컬럼과 validation 결과가 어긋납니다.

그래서 긴 문자열 필드는 Entity에서 다음처럼 명시했습니다.

```java
@Column(columnDefinition = "text")
private String content;
```

이 결정은 DB 스키마와 JPA Entity 검증 결과를 일치시키기 위한 것입니다.

## Health check

`/actuator/health/db`의 `UP`은 애플리케이션이 현재 DataSource로 DB에 정상 접속할 수 있다는 뜻입니다.

`UP` 자체가 테이블 생성 여부를 직접 의미하지는 않습니다. 테이블 생성 여부는 Flyway migration과 Hibernate validation 결과로 판단합니다.

이번 검증에서 확인한 순서는 다음과 같습니다.

1. Supabase DB 접속 성공.
2. Flyway `V1` migration 적용 또는 적용 완료 상태 확인.
3. Hibernate `ddl-auto=validate` 통과.
4. 애플리케이션 기동 성공.
5. `/actuator/health`에서 `db` component `UP` 확인.

따라서 Supabase dashboard에서 확인해야 하는 것은 Java Entity가 아니라 Entity에 대응되는 PostgreSQL table입니다.

## Redis health 비활성화

`adapter-out:persistence`에는 Redis starter가 포함되어 있습니다. 현재 코드에서 Redis 직접 사용처는 없지만, Spring Boot Actuator는 Redis starter가 있으면 Redis health indicator를 자동 등록할 수 있습니다.

실제 Supabase DB 연결 검증 중 전체 `/actuator/health`가 `DOWN`이 된 원인은 PostgreSQL이 아니라 로컬 Redis `localhost:6379` 접속 실패였습니다.

이번 작업 범위는 PostgreSQL 연결이므로 Supabase 프로필에서는 Redis health를 비활성화했습니다.

```yaml
management:
  health:
    redis:
      enabled: false
```

나중에 Redis를 실제 운영 의존성으로 사용하게 되면 Redis host, password, TLS 설정을 별도로 정의하고 health check를 다시 켜야 합니다.

## 검증 결과

다음 검증을 수행했습니다.

```bash
./gradlew :adapter-out:persistence:test :bootstrap:test :bootstrap:bootJar
```

결과는 성공입니다.

실제 Supabase 값으로도 다음을 확인했습니다.

1. `psql` 접속 성공.
2. `SPRING_PROFILES_ACTIVE=supabase`로 애플리케이션 기동 성공.
3. Flyway migration validation 성공.
4. Hibernate schema validation 성공.
5. `/actuator/health`에서 `db` component `UP` 확인.
6. `/actuator/health/db`에서 `UP` 확인.

## 운영 시 주의점

1. 실제 secret은 `.env.example`이나 repository에 기록하지 않습니다.
2. 배포 환경에는 `SPRING_PROFILES_ACTIVE=supabase`를 반드시 주입합니다.
3. Supabase direct connection이 안 되는 IPv4-only 환경에서는 session pooler를 사용합니다.
4. 새로운 테이블이나 컬럼 변경은 Hibernate `update`가 아니라 `V2__...sql` 같은 Flyway migration으로 추가합니다.
5. Redis를 운영에서 사용하기 시작하면 Supabase 프로필의 Redis health 비활성화 결정을 다시 검토합니다.

## 남은 결정

초기 데이터 seed는 아직 포함하지 않았습니다. 운영 또는 개발 초기 데이터가 필요해지면 `V1`에 섞지 말고 별도 migration이나 seed 전략으로 분리합니다.
