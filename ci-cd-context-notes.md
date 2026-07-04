# GitHub Actions CI/CD Context Notes

- Existing workflow file is `.github/workflows/ci.yml`.
- Existing CI triggers are `pull_request` to `develop` and `main`, plus `push` to `develop` and `main`.
- Deployment must be conditional so `develop` push and pull requests keep CI-only behavior.
- This Gradle repo is a multi-module Spring Boot project. The executable jar is produced by `:bootstrap:bootJar`.
- After running `./gradlew :bootstrap:bootJar --no-daemon`, the current executable jar path is `bootstrap/build/libs/bootstrap-0.0.1-SNAPSHOT.jar`.
- Runtime configuration requires `NEARBY_JWT_SECRET`. The `supabase` profile also requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.
- Optional runtime values include Kakao OIDC/native app settings, Solapi SMS settings, token TTL overrides, and profile-image S3/CDN settings.
- No `@ConfigurationProperties` classes are currently present.
- The workflow resolves the jar by checking the manifest `Implementation-Title: bootstrap` so stale local jars do not get uploaded.
- No Terraform files exist in this backend repository, so OIDC role and artifact bucket infrastructure cannot be added here.
