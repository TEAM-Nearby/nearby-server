# GitHub Actions CI/CD Checklist

- [x] Read all existing workflow files under `.github/workflows`.
- [x] Confirm Gradle structure and actual `bootJar` output path.
- [x] Review Spring runtime configuration and environment variable needs.
- [x] Preserve existing CI test, build, and branch triggers.
- [x] Add deploy path for `main` push and `workflow_dispatch` only.
- [x] Upload the verified boot jar to the deploy artifact S3 bucket.
- [x] Deploy through AWS SSM without SSH or SCP.
- [x] Verify workflow YAML syntax and Gradle build.
- [x] Commit the CI/CD workflow change.
