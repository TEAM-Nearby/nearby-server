# AGENTS.md

This file defines Codex working instructions for this repository. Before making changes, inspect the current code and build files, then apply the smallest change that preserves the modular monolith boundaries.

## Project Overview

- This is a Java 21, Spring Boot 3.5, Spring Modulith backend.
- The project uses a Gradle multi-module structure with a modular monolith and hexagonal architecture.
- The root project name is `nearby`.
- The current modules are `bootstrap`, `application`, `adapter-in:web`, `adapter-out:persistence`, `adapter-out:security`, `common`, and `logging`.
- For architecture background, see `docs/architecture/ADR-01-multimodule.md`.

## Module Responsibilities

- `bootstrap` owns application startup and runtime assembly.
  - Put `@SpringBootApplication`, Spring Boot bootstrap code, runtime configuration, actuator setup, and Modulith runtime setup here.
  - Do not use this module for HTTP, domain, use case, or adapter implementation details.
- `application` owns the hexagonal core.
  - Put domain models, business rules, use cases, application services, inbound ports, and outbound ports here.
  - Use packages such as `{feature}/domain/model`, `{feature}/application`, `{feature}/port/in`, and `{feature}/port/out`.
  - Do not depend on external technical details such as Spring Web, JPA, Redis, or Security.
- `adapter-in:web` owns the HTTP inbound adapter.
  - Put controllers, request/response DTOs, validation, HTTP status behavior, web exception handling, and OpenAPI-related code here.
  - Put feature HTTP code under `{feature}/adapter/in/web`.
  - Separate Swagger/OpenAPI documentation from controller implementation by using feature-specific `*Api` interfaces.
  - Do not put business rules in controllers or DTOs.
- `adapter-out:persistence` owns persistence outbound adapters.
  - Put JPA entities, Spring Data repositories, Redis integration, persistence mappers, and repository adapter implementations here.
  - Put feature persistence code under `{feature}/adapter/out/persistence`.
  - Do not let persistence models leak into the public `application` API.
- `adapter-out:security` owns security outbound adapters.
  - Put Spring Security configuration, token issuing, token verification, and security integration here.
  - Do not put business rules or persistence implementation details here.
- `common` owns only stable types and utilities shared across modules.
  - Do not use this module as a dumping ground for feature-specific convenience code.
- `logging` owns request logging infrastructure.
  - Put MDC, request id, logging filters, and log masking here.
  - Do not move servlet or Spring Web logging code into `common`.

## Dependency Direction

The current Gradle dependency direction is:

```text
bootstrap -> adapter-in:web, adapter-out:persistence, adapter-out:security, application, common, logging
adapter-in:web -> application, common, logging
adapter-out:persistence -> application, common, logging
adapter-out:security -> application, common, logging
application -> common
logging -> common
common -> none
```

- Do not add dependencies that violate this direction.
- `application` must not depend on `adapter-in:web`, `adapter-out:persistence`, `adapter-out:security`, `bootstrap`, or `logging`.
- Keep inbound adapters focused on external entry points, and keep outbound adapters focused on technical implementation.
- Before moving code to `common`, confirm that it is truly a stable shared concept across multiple modules.

## Modulith Package Boundaries

- Gradle modules are physical build boundaries, but Spring Modulith modules are logical feature boundaries.
- Spring Modulith analyzes top-level packages under `com.sopt.nearby`, such as `user`, `place`, `companion`, `security`, `shared`, and `logging`.
- Keep same-feature code under the same top-level feature package even when it lives in different Gradle modules.
- Do not directly reference another feature's internal implementation package.
- If a feature must collaborate with another feature, prefer an application service, port, or domain event over direct internal type access.
- Use `@NamedInterface` only for intentional public contracts. Do not add it just to make Modulith verification pass.

## Working Principles

- Before changing code, read the relevant files and nearby call sites first.
- Do not change structure based on assumptions. Use the current Gradle dependencies and package structure as evidence.
- Do not refactor, reformat, or delete dead code outside the requested scope.
- Do not revert existing user changes.
- Commit only when the user explicitly asks for a commit.
- When creating a new source file, put a one-line Korean comment on the first line that states the file's role.
- When responding in Korean, do not end Korean sentences with a colon.

## Implementation Standards

- Place a single feature change in the closest appropriate hexagonal boundary.
- Handle HTTP request and response shape changes in `adapter-in:web`.
- Keep business decisions in `application`.
- Put DB and Redis integration in `adapter-out:persistence`.
- Put authentication and authorization infrastructure in `adapter-out:security`.
- Put application startup and configuration assembly in `bootstrap`.
- Do not change behavior without tests. If testing is difficult, explain the reason clearly.

## API And Swagger Standards

- Keep REST controller implementation and Swagger documentation separated.
- Place HTTP controller code under `adapter-in/web/src/main/java/com/sopt/nearby/{feature}/adapter/in/web`.
- For each API feature, create a `{Feature}Api` interface for Swagger/OpenAPI annotations.
- Create the matching `{Feature}Controller` class in the same feature controller package and make it implement `{Feature}Api`.
- Put `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, and custom Swagger exception annotations on the `{Feature}Api` interface.
- Put `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PatchMapping`, `@DeleteMapping`, and request handling logic on the `{Feature}Controller` class.
- Keep request binding annotations needed by Spring MVC, such as `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`, and `@Valid`, on the controller method implementation.
- Do not put business rules in `{Feature}Api` or `{Feature}Controller`.
- Place request and response DTOs under the feature's `dto/request` and `dto/response` packages.

## Verification Commands

Run the smallest verification command that matches the scope of the change first.

```bash
./gradlew test
./gradlew compileJava
./gradlew :bootstrap:bootJar
```

- If code changed, run the relevant tests first. Run broader verification when the risk is higher.
- If only documentation changed, tests may be skipped, but explain why in the final response.
- When a command fails, read the actual error message, identify the cause, and then fix it.

## Review And PR Standards

- Align review judgment with the P1-P5 criteria in `.coderabbit.yaml`.
- P1 means a defect that must be fixed.
- P2 means a defect or design risk that should be strongly considered.
- P3 means an improvement that should be applied when practical.
- P4 means an optional improvement.
- P5 means a minor opinion.
- PR descriptions should clearly state the implementation, test results, and areas reviewers should focus on.

## Codex Response Rules

- If the user asks in Korean, respond in Korean.
- In the final response, report the verification commands that actually ran and their results.
- If any verification could not be run, state the reason specifically.
- If files were created or modified, clearly list the changed files.
