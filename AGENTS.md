# AGENTS.md

This file defines Codex working instructions for this repository. Before making changes, inspect the current code and build files, then apply the smallest change that preserves the modular monolith boundaries.

## Project Overview

- This is a Java 21, Spring Boot 3.5, Spring Modulith backend.
- The project uses a Gradle multi-module structure.
- The root project name is `nearby`.
- The current modules are `app`, `api`, `domain`, `adapter`, and `common`.

## Module Responsibilities

- `app` owns application startup and runtime assembly.
  - Put `@SpringBootApplication`, Spring Boot bootstrap code, runtime configuration, actuator setup, and Modulith runtime setup here.
  - Do not use this module for API, domain, or adapter implementation details.
- `api` owns the HTTP API boundary.
  - Put controllers, request/response DTOs, validation, HTTP status behavior, and OpenAPI-related code here.
  - Separate Swagger/OpenAPI documentation from controller implementation by using feature-specific `*Api` interfaces.
  - Do not put business rules in controllers or DTOs.
- `domain` owns business rules and domain models.
  - Put core policies, state transitions, domain services, and domain events here.
  - Do not depend on external technical details such as Spring Web, JPA, Redis, or Security.
- `adapter` owns infrastructure and external system integration.
  - Put JPA, Redis, Security, external API clients, and persistence mapping here.
  - Do not let infrastructure models leak into the public `domain` API.
- `common` owns only stable types and utilities shared across modules.
  - Do not use this module as a dumping ground for feature-specific convenience code.

## Dependency Direction

The current Gradle dependency direction is:

```text
app -> api, domain, adapter, common
api -> domain, common
adapter -> domain, common
domain -> common
common -> none
```

- Do not add dependencies that violate this direction.
- `domain` must not depend on `api` or `adapter`.
- Keep `api` focused on the HTTP representation layer, and avoid direct dependencies on infrastructure implementations.
- Before moving code to `common`, confirm that it is truly a stable shared concept across multiple modules.

## Working Principles

- Before changing code, read the relevant files and nearby call sites first.
- Do not change structure based on assumptions. Use the current Gradle dependencies and package structure as evidence.
- Do not refactor, reformat, or delete dead code outside the requested scope.
- Do not revert existing user changes.
- Commit only when the user explicitly asks for a commit.
- When creating a new source file, put a one-line Korean comment on the first line that states the file's role.
- When responding in Korean, do not end Korean sentences with a colon.

## Implementation Standards

- Place a single feature change in the closest appropriate module.
- Handle HTTP request and response shape changes in `api`.
- Keep business decisions in `domain`.
- Put DB, Redis, Security, and external API integration in `adapter`.
- Put application startup and configuration assembly in `app`.
- Do not change behavior without tests. If testing is difficult, explain the reason clearly.

## API And Swagger Standards

- Keep REST controller implementation and Swagger documentation separated.
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
./gradlew :app:bootJar
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
