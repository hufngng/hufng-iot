# Build context is the repo root (see infra/docker-compose.yml) so this stage
# can see hufng-common, which hufng-ingest depends on via project(":hufng-common") —
# there's no shared jar published anywhere else for it to resolve.
FROM gradle:8.6-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon -q :hufng-ingest:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
# Plain `jar` task is disabled, so build/libs holds only the executable bootJar
COPY --from=build /workspace/hufng-ingest/build/libs/hufng-ingest-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
