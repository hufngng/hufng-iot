# Build context is the repo root (see infra/docker-compose.yml) so this stage
# can see hufng-common, which hufng-notification depends on via project(":hufng-common") —
# there's no shared jar published anywhere else for it to resolve.
FROM gradle:8.6-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon -q :hufng-notification:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
# Plain `jar` task is disabled, so build/libs holds only the executable bootJar
COPY --from=build /workspace/hufng-notification/build/libs/hufng-notification-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
