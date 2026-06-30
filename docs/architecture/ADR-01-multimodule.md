# Nearby 멀티 모듈 헥사고날 아키텍처 전환 기록

## 목적

Nearby는 Java 21, Spring Boot 3.5, Spring Modulith 기반의 단일 애플리케이션입니다. 이번 구조 전환의 목표는 서비스를 MSA로 분리하는 것이 아니라, 하나의 배포 단위 안에서 업무 규칙, 외부 입출력, 실행 조립 책임을 분명하게 나누는 것입니다.

이 문서는 PR 본문에 모두 담기 어려운 구조 판단 근거와 후속 개발 규칙을 기록합니다.

## 기존 구조에서 불편했던 점

기존 구조는 `app`, `api`, `domain`, `adapter`, `common`으로 나뉘어 있었지만, Gradle 모듈 경계와 실제 코드 책임이 완전히 일치하지 않았습니다.

1. `app`의 의미가 실행 책임인지, 런타임 조립 책임인지, 일반 애플리케이션 코드가 들어가는 위치인지 모호했습니다.
2. `domain` 안의 repository 인터페이스는 구현체와 분리되어 있었지만, 헥사고날 아키텍처 관점의 outbound port라는 의도가 패키지 이름에서 충분히 드러나지 않았습니다.
3. `adapter` 안에 JPA, Redis, Security 같은 기술 구현이 함께 있어 adapter 내부에서도 변경 영향 범위를 빠르게 판단하기 어려웠습니다.
4. `api`는 실제로 HTTP inbound adapter에 가까웠지만, 이름만으로는 외부 진입점이라는 의미가 약했습니다.
5. `common`에는 여러 모듈이 공유하는 안정적인 계약만 들어가야 하는데, 기준이 느슨해지면 기술 구현을 공유하기 위한 우회 경로가 되기 쉽습니다.
6. JPA Entity가 다른 업무 모듈의 Entity를 직접 참조하면 Spring Modulith가 기대하는 모듈 내부 구현 은닉 규칙과 충돌하기 쉽습니다.
7. 요청 로그, request id, 민감정보 마스킹은 여러 웹 요청에서 공통으로 필요한 횡단 책임인데, 특정 feature나 adapter에 넣으면 책임 위치가 흐려집니다.

## 최종 Gradle 모듈 구조

```text
nearby
├── bootstrap
├── application
├── adapter-in
│   └── web
├── adapter-out
│   ├── persistence
│   └── security
├── common
└── logging
```

Gradle 모듈은 물리적인 빌드 경계입니다. 각 모듈은 어떤 외부 라이브러리를 사용할 수 있는지, 어떤 모듈에 의존할 수 있는지를 강제합니다.

## 모듈별 책임

| 모듈 | 책임 | 판단 근거 |
| --- | --- | --- |
| `bootstrap` | Spring Boot 실행 진입점, 런타임 조립, 설정 로딩, actuator, Spring Modulith 런타임 구성 | 실행 가능한 애플리케이션을 만드는 책임은 비즈니스 코드나 adapter 구현과 분리되어야 합니다. 이 모듈만 Spring Boot 애플리케이션 플러그인을 사용합니다. |
| `application` | 도메인 모델, 업무 규칙, outbound port | 헥사고날 아키텍처의 중심은 외부 기술에 의존하지 않는 애플리케이션 코어입니다. 현재는 도메인 모델과 port가 중심이고, 추후 use case service가 생기면 이 모듈에 둡니다. |
| `adapter-in:web` | HTTP controller, request/response DTO, validation, 웹 예외 처리, Swagger 설정 | HTTP 요청은 애플리케이션 코어로 들어오는 inbound adapter입니다. Spring MVC와 OpenAPI 의존성은 이 모듈 안에 머무는 것이 적절합니다. |
| `adapter-out:persistence` | JPA Entity, Spring Data repository, Redis, persistence mapper, outbound port 구현 | 데이터 저장소는 애플리케이션 코어가 의존하면 안 되는 외부 기술입니다. JPA와 Redis 의존성을 별도 outbound adapter에 둡니다. |
| `adapter-out:security` | Spring Security 기반 인증 및 인가 구현 | 보안 구현은 persistence와 다른 기술 축입니다. 인증 필터, security context, token 처리 같은 책임이 커질 수 있어 별도 outbound adapter로 분리합니다. |
| `common` | 여러 모듈에서 공유해도 되는 안정적인 예외, port 보조 타입 | 특정 feature나 기술 구현에 종속되지 않는 계약만 둡니다. 편의성 때문에 코드를 모으는 장소로 사용하지 않습니다. |
| `logging` | MDC, request id, 요청 로그 필터, 민감정보 마스킹 | 로깅은 업무 규칙이 아니라 횡단 인프라 책임입니다. Spring Web과 servlet 필터에 의존하므로 `common`보다 별도 모듈이 적절합니다. |

## 의존 방향

```text
bootstrap -> adapter-in:web
bootstrap -> adapter-out:persistence
bootstrap -> adapter-out:security
bootstrap -> application
bootstrap -> common
bootstrap -> logging

adapter-in:web -> application
adapter-in:web -> common
adapter-in:web -> logging

adapter-out:persistence -> application
adapter-out:persistence -> common
adapter-out:persistence -> logging

adapter-out:security -> application
adapter-out:security -> common
adapter-out:security -> logging

application -> common
logging -> common
common -> none
```

핵심 규칙은 `application`이 외부 기술을 모르는 것입니다. Spring Web, JPA, Redis, Security 의존성은 adapter 쪽에 있고, `application`은 port 인터페이스를 통해 필요한 기능만 표현합니다.

## Spring Modulith와 Gradle 멀티 모듈의 관계

Spring Modulith는 Spring 애플리케이션 안의 논리적 애플리케이션 모듈을 분석하고 검증하는 도구입니다. Nearby에서는 Gradle 모듈과 Spring Modulith 모듈이 같은 개념이 아닙니다.

Gradle 모듈은 기술 계층과 의존 방향을 강제합니다.

```text
application
adapter-in:web
adapter-out:persistence
adapter-out:security
bootstrap
common
logging
```

Spring Modulith 모듈은 `com.sopt.nearby` 아래의 최상위 업무 패키지를 기준으로 논리적 경계를 봅니다.

```text
com.sopt.nearby.user
com.sopt.nearby.place
com.sopt.nearby.companion
com.sopt.nearby.security
com.sopt.nearby.shared
com.sopt.nearby.logging
```

이렇게 나눈 이유는 두 검증 축이 서로 다르기 때문입니다.

1. Gradle은 `application`이 `adapter-out:persistence`를 참조하지 못하게 막습니다.
2. Spring Modulith는 `user` 모듈이 `companion` 모듈의 내부 구현을 직접 참조하지 않는지 검증합니다.
3. 같은 feature의 domain model, port, adapter 구현은 Gradle 모듈상으로는 다른 위치에 있어도 `com.sopt.nearby.user`처럼 같은 최상위 feature 패키지 아래에 놓입니다.

즉, Nearby의 구조는 기술 계층 기준의 물리적 분리와 업무 feature 기준의 논리적 분리를 함께 사용합니다. 이 방식은 Spring Modulith를 적용한 모듈러 모놀리스 구조에서 특히 중요합니다. 단일 기준으로만 나누면 기술 의존성은 잘 보이지만 업무 모듈 침범을 놓치거나, 반대로 업무 경계는 보이지만 JPA와 Web 의존성이 코어로 새어 들어올 수 있습니다.

## 현재 패키지 배치

현재 `application` 모듈에는 업무별 도메인 모델과 outbound port가 있습니다.

```text
application/src/main/java/com/sopt/nearby
├── user
│   ├── domain/model
│   └── port/out
├── place
│   ├── domain/model
│   └── port/out
└── companion
    ├── domain/model
    └── port/out
```

현재 `adapter-out:persistence` 모듈에는 업무별 persistence adapter가 있습니다.

```text
adapter-out/persistence/src/main/java/com/sopt/nearby
├── user/adapter/out/persistence
│   ├── entity
│   ├── mapper
│   └── repository
├── place/adapter/out/persistence
│   ├── entity
│   ├── mapper
│   └── repository
├── companion/adapter/out/persistence
│   ├── entity
│   ├── mapper
│   └── repository
└── shared/adapter/out/persistence/support
```

현재 `adapter-in:web` 모듈에는 아직 feature controller보다 공통 웹 구성이 먼저 배치되어 있습니다.

```text
adapter-in/web/src/main/java/com/sopt/nearby/shared/adapter/in/web
├── config
├── exception
└── response
```

추후 HTTP API가 추가되면 다음과 같은 위치에 둡니다.

```text
adapter-in/web/src/main/java/com/sopt/nearby/{feature}/adapter/in/web
├── controller
├── dto/request
└── dto/response
```

## `@NamedInterface` 사용 기준

Spring Modulith는 기본적으로 다른 모듈의 내부 패키지를 직접 참조하지 못하도록 봅니다. 다른 모듈에서 사용해도 되는 타입은 공개 인터페이스로 명확히 표시해야 합니다.

현재 Nearby는 다음 패키지를 명시적으로 공개합니다.

```text
common.exception
common.port
shared.adapter.out.persistence.support
```

이 판단의 근거는 다음과 같습니다.

1. `common.exception`은 여러 모듈에서 공통으로 던지거나 처리할 수 있는 안정적인 예외 계약입니다.
2. `common.port`는 port 구현에 필요한 공통 계약을 담는 위치입니다.
3. `shared.adapter.out.persistence.support`는 persistence adapter 내부에서 반복되는 지원 타입을 제공하기 위한 공개 지점입니다.

반대로 feature의 JPA Entity, mapper, repository 구현체는 공개하지 않습니다. 이 타입들은 persistence adapter 내부 구현이므로 다른 feature나 application core가 직접 의존하면 안 됩니다.

## JPA Entity 참조 규칙

업무 모듈 간 관계가 필요하더라도 JPA Entity를 직접 참조하는 방식은 피합니다. 예를 들어 `companion`의 Entity가 `user`의 Entity를 필드로 직접 들고 있으면 persistence layer에서는 편할 수 있지만, Spring Modulith 관점에서는 `companion` 모듈이 `user` 모듈의 내부 구현을 직접 참조하게 됩니다.

Nearby에서는 이런 관계를 우선 scalar id로 표현합니다.

```java
private Long userAccountId;
```

이 방식의 장점은 다음과 같습니다.

1. feature 간 내부 구현 의존이 줄어듭니다.
2. JPA 연관관계 편의성보다 모듈 경계가 우선됩니다.
3. 추후 도메인 이벤트나 application service를 통해 모듈 간 협력을 명시적으로 만들 수 있습니다.
4. Spring Modulith 검증이 실제 아키텍처 경계를 깨는 참조를 더 잘 잡아낼 수 있습니다.

## `logging` 모듈이 필요한 이유

`logging` 모듈은 현재 Nearby의 비즈니스 기능을 직접 만들지는 않습니다. 그러나 운영 환경에서 문제를 추적하고, API 요청 흐름을 연결하고, 기본적인 민감정보 노출 위험을 줄이기 위해 필요한 횡단 인프라입니다.

현재 구현된 구성요소는 다음과 같습니다.

| 구성요소 | 책임 |
| --- | --- |
| `RequestIdGenerator` | 요청에 사용할 UUID 기반 식별자를 생성합니다. |
| `SensitiveLogMasker` | bearer token, 국내 휴대폰 번호, password 파라미터를 기본 마스킹합니다. |
| `MdcLoggingFilter` | 요청 시작과 종료 로그를 남기고, `X-Request-Id`를 응답 헤더와 MDC에 저장합니다. |

MDC는 Mapped Diagnostic Context의 약자입니다. SLF4J와 Logback 계열에서 사용하는 thread-local 기반 key-value 저장소이며, 같은 요청을 처리하는 동안 로그 패턴에 `requestId`를 함께 출력할 수 있게 해줍니다.

request id는 요청 하나를 식별하기 위한 값입니다. 클라이언트가 `X-Request-Id` 헤더를 보내면 그 값을 사용하고, 없으면 서버가 새 UUID를 생성합니다. 이 값은 응답 헤더에도 실리므로 클라이언트 로그와 서버 로그를 같은 식별자로 연결할 수 있습니다.

`logging`을 `common`에 넣지 않은 이유는 명확합니다. `common`은 안정적인 타입과 계약을 공유하는 모듈이어야 하는데, `MdcLoggingFilter`는 servlet filter와 Spring Web에 의존합니다. 이런 기술 의존성을 `common`에 넣으면 `application` 같은 코어 모듈이 실수로 웹 기술에 가까워지는 길이 열립니다.

현재 `SensitiveLogMasker`는 모든 로그를 자동으로 마스킹하는 전역 보안 장치가 아닙니다. 지금 보장하는 범위는 `MdcLoggingFilter`가 기록하는 요청 URI에 대한 기본 마스킹입니다. 향후 request body, response body, 애플리케이션 로그 전체를 마스킹하려면 별도 로깅 appender, structured logging 정책, 민감 필드 분류 규칙이 추가로 필요합니다.

## 검증 방식

Spring Modulith 검증은 다음 테스트에서 수행합니다.

```java
ApplicationModules.of(NearbyApplication.class).verify();
```

테스트 위치는 다음과 같습니다.

```text
bootstrap/src/test/java/com/sopt/nearby/architecture/ApplicationModulithTest.java
```

이 테스트의 의미는 다음과 같습니다.

1. Spring Boot 진입점 기준으로 애플리케이션 모듈을 분석합니다.
2. 모듈 간 허용되지 않은 내부 패키지 참조를 확인합니다.
3. 모듈 사이의 순환 의존이나 공개 경계 위반을 조기에 발견합니다.
4. Gradle 컴파일 성공만으로는 잡기 어려운 논리적 모듈 침범을 테스트 단계에서 확인합니다.

이번 구조 전환 후 확인한 명령은 다음과 같습니다.

```bash
./gradlew :logging:test
./gradlew :bootstrap:test --tests '*ApplicationModulithTest'
./gradlew test
./gradlew :bootstrap:bootJar
```

## 후속 개발 규칙

1. 새로운 업무 도메인은 `application/src/main/java/com/sopt/nearby/{feature}` 아래에서 시작합니다.
2. 도메인 모델과 업무 규칙은 `application`에 두고, Spring Web, JPA, Redis, Security 의존성을 넣지 않습니다.
3. HTTP controller와 request/response DTO는 `adapter-in:web`에 둡니다.
4. JPA Entity, Spring Data repository, Redis repository, mapper는 `adapter-out:persistence`에 둡니다.
5. 인증과 인가 관련 Spring Security 구현은 `adapter-out:security`에 둡니다.
6. `common`에는 feature에 종속되지 않고 여러 모듈에서 장기적으로 공유할 계약만 둡니다.
7. feature의 내부 구현을 다른 feature에서 직접 참조해야 할 필요가 생기면 먼저 application service, port, domain event 중 어떤 협력 방식이 적절한지 검토합니다.
8. Spring Modulith 검증을 통과시키기 위해 `@NamedInterface`를 무리하게 늘리지 않습니다. 공개 인터페이스가 많아진다는 것은 모듈 내부 구현이 외부로 새고 있다는 신호일 수 있습니다.
