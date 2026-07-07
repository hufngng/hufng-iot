# Build context is the repo root (see infra/docker-compose.yml) so this stage
# can see hufng-common, which hufng-ingest depends on via the reactor — there's
# no shared jar published anywhere else for it to resolve.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl hufng-ingest -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/hufng-ingest/target/hufng-ingest-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
