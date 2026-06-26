# 애플리케이션 실행 이미지를 빌드하는 Dockerfile
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY app ./app
COPY api ./api
COPY domain ./domain
COPY adapter ./adapter
COPY common ./common

RUN ./gradlew :app:bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
